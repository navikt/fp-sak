package no.nav.foreldrepenger.jsonfeed;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
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
import no.nav.vedtak.felles.xml.vedtak.v2.Vedtak;



public abstract class AbstractHendelsePublisererTjeneste implements HendelsePublisererTjeneste {
    private static final Logger log = LoggerFactory.getLogger(AbstractHendelsePublisererTjeneste.class);
    protected static final String FAGSAK_PREFIX = "FS";
    protected static final String VEDTAK_PREFIX = "VT";

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private EtterkontrollRepository etterkontrollRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FeedRepository feedRepository;
    Map<String, FagsakYtelseType> =Map.of()

    public AbstractHendelsePublisererTjeneste() {
        //Creatively Diversified Investments
    }

    public AbstractHendelsePublisererTjeneste(BehandlingsresultatRepository behandlingsresultatRepository,
                                              EtterkontrollRepository etterkontrollRepository,
                                              BehandlingRepositoryProvider behandlingRepositoryProvider,
                                              FeedRepository feedRepository) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.etterkontrollRepository = etterkontrollRepository;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository()
        this.beregningsresultatRepository = behandlingRepositoryProvider.getBeregningsresultatRepository();
        this.feedRepository = feedRepository;
    }

    class TypeMelding {
        private static final  String INNVILGET = "INNV";
        private static final  String ENDRET = "ENDR";
        private static final String OPPHØRT = "OPHRT";

        private FagsakYtelseType fagsakYtelseType;

        TypeMelding( String type, FagsakYtelseType fagsakYtelseType) {
            this.meldingstype = meldingstype;
            this.fagsakYtelseType = fagsakYtelseType;
        }
    }

    @Override
    public void lagreVedtak(BehandlingVedtak vedtak) {
        log.info("lagrer utgående hendelse for vedtak {}", vedtak.getId());

        Behandling behandling = behandlingRepository.hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());
        BehandlingType behandlingType = behandling.getType();

        if (hendelseEksistererAllerede(vedtak)) {
            log.debug("Skipper lagring av hendelse av vedtakId {} fordi den allerede eksisterer", vedtak.getId());
            return;
        }

        if (vedtak.getVedtakResultatType().equals(VedtakResultatType.AVSLAG) || vedtak.getVedtakResultatType().equals(VedtakResultatType.OPPHØR)){
            etterkontrollRepository.avflaggDersomEksisterer(behandling.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);
        }

        //Disse trigger ikke hendelser
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsak().getYtelseType())
            || !behandling.getType().erYtelseBehandlingType()
            || vedtak.getBehandlingsresultat().getBehandlingResultatType().erHenlagt()
            || erAvslagPåAvslag(behandling)) { //usikker på om vi trenger denne?
            return;
        }

        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsak().getYtelseType())) {
            nyDoLagreVedtakFP(vedtak, behandling);
        }
        else {
            nyDoLagreVedtakSVP(vedtak, behandling);
        }
    }

    private void nyDoLagreVedtakFP(BehandlingVedtak vedtak, Behandling behandling) {
        Optional<LocalDateInterval> innvilgetPeriode = finnPeriode(behandling);
        Optional<LocalDateInterval> orginalPeriode = behandling.getOriginalBehandling().flatMap(this::finnPeriode);

        Meldingstype meldingstype = mapMeldingstype(behandling.getFagsak().getYtelseType(), innvilgetPeriode, orginalPeriode);

        if (meldingstype == null) {
            //ingen hendelse, ingen endring i perioder
            return;
        }
        Innhold innhold = nyMapVedtakTilInnhold(behandling, meldingstype, innvilgetPeriode.orElse(orginalPeriode.get()));

        String payloadJason = JsonMapper.toJson(innhold);

        FpVedtakUtgåendeHendelse fpVedtakUtgåendeHendelse = FpVedtakUtgåendeHendelse.builder()
            .aktørId(behandling.getAktørId().getId())
            .payload(payloadJason)
            .type(meldingstype.getType())
            .kildeId(VEDTAK_PREFIX + vedtak.getId())
            .build();
        feedRepository.lagre(fpVedtakUtgåendeHendelse);
    }


    private Meldingstype mapMeldingstype (TypeMelding typeMelding, Optional<LocalDateInterval> innvilgetPeriode, Optional<LocalDateInterval> orginalPeriode) {

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

    private void nyDoLagreVedtakSVP(BehandlingVedtak vedtak, Behandling behandling) {

        Meldingstype meldingstype;

        Optional<LocalDateInterval> innvilgetPeriode = finnPeriode(behandling);
        Optional<LocalDateInterval> orginalPeriode = behandling.getOriginalBehandling().flatMap(this::finnPeriode);

        if (innvilgetPeriode.isEmpty() && orginalPeriode.isEmpty()) {
            //ingen hendelse
            return;
        }

        if (innvilgetPeriode.isPresent() && orginalPeriode.isEmpty()) {
            meldingstype = Meldingstype.SVANGERSKAPSPENGER_INNVILGET;
        } else if (innvilgetPeriode.isEmpty() && orginalPeriode.isPresent()) {
            meldingstype = Meldingstype.SVANGERSKAPSPENGER_OPPHOERT;
        } else if (!innvilgetPeriode.get().equals(orginalPeriode.get())) {
            meldingstype = Meldingstype.SVANGERSKAPSPENGER_ENDRET;
        } else {
            //revurdering, men ingen endring i utbetalingsperiode
            return;
        }

        Innhold innhold = nyMapVedtakTilInnhold(behandling, meldingstype, innvilgetPeriode.orElse(orginalPeriode.get()));

        String payloadJason = JsonMapper.toJson(innhold);

        SvpVedtakUtgåendeHendelse svpVedtakUtgåendeHendelse = SvpVedtakUtgåendeHendelse.builder()
            .aktørId(behandling.getAktørId().getId())
            .payload(payloadJason)
            .type(meldingstype.getType())
            .kildeId(VEDTAK_PREFIX + vedtak.getId())
            .build();
        feedRepository.lagre(svpVedtakUtgåendeHendelse);
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
        } else if (Meldingstype.FORELDREPENGER_OPPHOERT.equals(type)) {
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


    protected abstract boolean uttakFomEllerTomErEndret(Long orginalbehId, Long behandlingId);

    protected boolean erInnvilgetRevurdering(BehandlingType behandlingType, BehandlingResultatType behandlingResultatType) {
        return BehandlingType.REVURDERING.equals(behandlingType) && BehandlingResultatType.INNVILGET.equals(behandlingResultatType);
    }

    protected boolean erFørstegangsSøknad(BehandlingType behandlingType) {
        return BehandlingType.FØRSTEGANGSSØKNAD.equals(behandlingType);
    }

    protected boolean erOpphørtRevurdering(BehandlingType behandlingType, BehandlingResultatType behandlingResultatType) {
        return BehandlingType.REVURDERING.equals(behandlingType) && BehandlingResultatType.OPPHØR.equals(behandlingResultatType);
    }

    private boolean erBeslutningsvedtak(BehandlingType behandlingType, BehandlingResultatType behandlingResultatType) {
        return BehandlingType.REVURDERING.equals(behandlingType) && BehandlingResultatType.INGEN_ENDRING.equals(behandlingResultatType);
    }

    private boolean erEndring(BehandlingType behandlingType) {
        return BehandlingType.REVURDERING.equals(behandlingType);
    }

    private boolean erAvslagPåAvslag(Behandling behandling) {
        if(behandling.erRevurdering()) {
            Optional<Behandling> origBehandling = behandling.getOriginalBehandling();
            if (origBehandling.isPresent()) {
                return erAvslåttBehandling(behandling) && erAvslåttBehandling(origBehandling.get());
            }
        }
        return false;
    }

    private boolean erAvslåttBehandling(Behandling behandling) {
        if (hentBehandlingsresultat(behandling).isPresent()) {
            return hentBehandlingsresultat(behandling).get().isBehandlingsresultatAvslått();
        }
        return false;
    }

    private Optional<Behandlingsresultat> hentBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
    }

}
