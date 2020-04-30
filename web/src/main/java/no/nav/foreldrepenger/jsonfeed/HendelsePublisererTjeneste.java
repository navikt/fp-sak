package no.nav.foreldrepenger.jsonfeed;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.feed.FeedRepository;
import no.nav.foreldrepenger.domene.feed.FpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.feed.SvpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerEndret;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerInnvilget;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerOpphoert;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Innhold;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Meldingstype;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.SvangerskapspengerEndret;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.SvangerskapspengerInnvilget;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.SvangerskapspengerOpphoert;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;
import no.nav.fpsak.tidsserie.LocalDateInterval;


@ApplicationScoped
public class HendelsePublisererTjeneste {
    private static final Logger log = LoggerFactory.getLogger(HendelsePublisererTjeneste.class);
    protected static final String VEDTAK_PREFIX = "VT";

    private EtterkontrollRepository etterkontrollRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FeedRepository feedRepository;

    public HendelsePublisererTjeneste() {
        //Creatively Diversified Investments
    }

    @Inject
    public HendelsePublisererTjeneste(EtterkontrollRepository etterkontrollRepository,
                                      BehandlingRepositoryProvider behandlingRepositoryProvider,
                                      FeedRepository feedRepository) {
        this.etterkontrollRepository = etterkontrollRepository;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = behandlingRepositoryProvider.getBeregningsresultatRepository();
        this.feedRepository = feedRepository;
    }

    public void lagreVedtak(BehandlingVedtak vedtak) {
        log.info("lagrer utgående hendelse for vedtak {}", vedtak.getId());

        Behandling behandling = behandlingRepository.hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());

        if (hendelseEksistererAllerede(vedtak)) {
            log.debug("Skipper lagring av hendelse av vedtakId {} fordi den allerede eksisterer", vedtak.getId());
            return;
        }

        if (vedtak.getVedtakResultatType().equals(VedtakResultatType.AVSLAG) || vedtak.getVedtakResultatType().equals(VedtakResultatType.OPPHØR)){
            etterkontrollRepository.avflaggDersomEksisterer(behandling.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);
        }

        //Disse trigger ikke hendelser
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())
            || !behandling.getType().erYtelseBehandlingType()
            || vedtak.getBehandlingsresultat().getBehandlingResultatType().erHenlagt()) {
            return;
        }

            nyDoLagreVedtak(vedtak, behandling);
    }

    private void nyDoLagreVedtak(BehandlingVedtak vedtak, Behandling behandling) {
        Optional<LocalDateInterval> innvilgetPeriode = finnPeriode(behandling);
        Optional<LocalDateInterval> orginalPeriode = behandling.getOriginalBehandling().flatMap(this::finnPeriode);

        Meldingstype meldingstype = mapMeldingstype(behandling.getFagsakYtelseType(), innvilgetPeriode, orginalPeriode);

        if (meldingstype == null) {
            //ingen hendelse, ingen endring i perioder
            return;
        }
        Innhold innhold = nyMapVedtakTilInnhold(behandling, meldingstype, innvilgetPeriode.orElse(orginalPeriode.get()));

        String payloadJason = JsonMapper.toJson(innhold);

        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            FpVedtakUtgåendeHendelse fpVedtakUtgåendeHendelse = FpVedtakUtgåendeHendelse.builder()
                .aktørId(behandling.getAktørId().getId())
                .payload(payloadJason)
                .type(meldingstype.getType())
                .kildeId(VEDTAK_PREFIX + vedtak.getId())
                .build();
            feedRepository.lagre(fpVedtakUtgåendeHendelse);
        } else {
            SvpVedtakUtgåendeHendelse svpVedtakUtgåendeHendelse= SvpVedtakUtgåendeHendelse.builder()
                .aktørId(behandling.getAktørId().getId())
                .payload(payloadJason)
                .type(meldingstype.getType())
                .kildeId(VEDTAK_PREFIX + vedtak.getId())
                .build();
            feedRepository.lagre(svpVedtakUtgåendeHendelse);
        }
    }


    private Meldingstype mapMeldingstype (FagsakYtelseType ytelseType, Optional<LocalDateInterval> innvilgetPeriode, Optional<LocalDateInterval> orginalPeriode) {

        Meldingstype meldingstype;

        if (innvilgetPeriode.isEmpty() && orginalPeriode.isEmpty()) {
            //ingen hendelse
            return null;
        }

        if (innvilgetPeriode.isPresent() && orginalPeriode.isEmpty()) {
            if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
                meldingstype = Meldingstype.FORELDREPENGER_INNVILGET;
            } else {
                meldingstype = Meldingstype.SVANGERSKAPSPENGER_INNVILGET;
                }
        } else if (innvilgetPeriode.isEmpty() && orginalPeriode.isPresent()) {
                if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
                    meldingstype = Meldingstype.FORELDREPENGER_OPPHOERT;
            } else {
                meldingstype = Meldingstype.SVANGERSKAPSPENGER_OPPHOERT;
                }
        } else if (!innvilgetPeriode.get().equals(orginalPeriode.get())) {
                if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
                    meldingstype = Meldingstype.FORELDREPENGER_ENDRET;
            } else {
                meldingstype = Meldingstype.SVANGERSKAPSPENGER_ENDRET;
                }
        } else {
            //revurdering, men ingen endring i utbetalingsperiode
            return null;
        }
         return meldingstype;
    }

   private Optional<LocalDateInterval> finnPeriode(Behandling behandling ){
        Optional<LocalDate> førsteUtbetDato = finnMinsteUtbetDato(behandling.getId());
        Optional<LocalDate> sisteUtbetDato = finnSisteUtbetDato(behandling.getId());

        if (førsteUtbetDato.isPresent() && sisteUtbetDato.isPresent()) {
            return Optional.of(new LocalDateInterval(førsteUtbetDato.get(), sisteUtbetDato.get()));
        }
        else {
            return Optional.empty();
        }
    }

    private Innhold nyMapVedtakTilInnhold(Behandling behandling, Meldingstype meldingstype, LocalDateInterval utbetPeriode ) {
        Innhold innhold;
        String type = meldingstype.getType();

        if (Meldingstype.FORELDREPENGER_INNVILGET.equals(type)) {
            innhold = new ForeldrepengerInnvilget();
        } else if (Meldingstype.FORELDREPENGER_OPPHOERT.equals(meldingstype)) {
            innhold = new ForeldrepengerOpphoert();
        } else if (Meldingstype.FORELDREPENGER_ENDRET.equals(type)) {
            innhold = new ForeldrepengerEndret();
        } else if (Meldingstype.SVANGERSKAPSPENGER_INNVILGET.equals(type)) {
            innhold = new SvangerskapspengerInnvilget();
        } else if (Meldingstype.SVANGERSKAPSPENGER_OPPHOERT.equals(type)) {
            innhold = new SvangerskapspengerOpphoert();
        } else {
            innhold = new SvangerskapspengerEndret();
        }

        innhold.setFoersteStoenadsdag(utbetPeriode.getFomDato());
        innhold.setSisteStoenadsdag(utbetPeriode.getTomDato());
        innhold.setAktoerId(behandling.getAktørId().getId());
        innhold.setGsakId(behandling.getFagsak().getSaksnummer().getVerdi());

        return innhold;
    }

    private Optional<LocalDate> finnMinsteUtbetDato(Long behandlingId) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);

        return berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder())
            .map(VirkedagUtil::fomVirkedag);
    }

    private Optional<LocalDate> finnSisteUtbetDato(Long behandlingId) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);
        return berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder())
            .map(VirkedagUtil::tomVirkedag);
    }

    private boolean hendelseEksistererAllerede(BehandlingVedtak vedtak) {
        return feedRepository.harHendelseMedKildeId(SvpVedtakUtgåendeHendelse.class, VEDTAK_PREFIX + vedtak.getId());
    }
}
