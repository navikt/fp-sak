package no.nav.foreldrepenger.jsonfeed;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.feed.FeedRepository;
import no.nav.foreldrepenger.domene.feed.FpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.feed.SvpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerEndret;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerInnvilget;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerOpphoert;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Innhold;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Meldingstype;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.SvangerskapspengerEndret;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.SvangerskapspengerInnvilget;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.SvangerskapspengerOpphoert;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class HendelsePublisererTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(HendelsePublisererTjeneste.class);
    private static final String VEDTAK_PREFIX = "VT";

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FeedRepository feedRepository;

    public HendelsePublisererTjeneste() {
        // Creatively Diversified Investments
    }

    @Inject
    public HendelsePublisererTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
            FeedRepository feedRepository) {
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = behandlingRepositoryProvider.getBeregningsresultatRepository();
        this.feedRepository = feedRepository;
    }

    public void lagreVedtak(BehandlingVedtak vedtak) {
        LOG.info("Utgående feed-hendelse inngang for vedtak {}", vedtak.getId());

        var behandling = behandlingRepository.hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());

        if (hendelseEksistererAllerede(vedtak)) {
            LOG.info("Utgående feed-hendelse vedtakId {} eksisterer allerede", vedtak.getId());
            return;
        }

        LOG.info("Utgående feed-hendelse vedtakId {} nytt vedtak", vedtak.getId());

        // Disse trigger ikke hendelser
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())
                || !behandling.getType().erYtelseBehandlingType()
                || vedtak.getBehandlingsresultat().getBehandlingResultatType().erHenlagt()) {
            LOG.info("Utgående feed-hendelse vedtakId {} ikke FP/SVP", vedtak.getId());
            return;
        }

        var innvilgetPeriode = finnPeriode(behandling.getId());
        var orginalPeriode = behandling.getOriginalBehandlingId().flatMap(this::finnPeriode);

        if (innvilgetPeriode.isEmpty() && orginalPeriode.isEmpty()) {
            // ingen hendelse
            LOG.info("Utgående feed-hendelse vedtakId {} ingen data", vedtak.getId());
            return;
        }

        LOG.info("Utgående feed-hendelse vedtakId {} funnet perioder", vedtak.getId());

        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            doLagreVedtakFP(vedtak, behandling, innvilgetPeriode, orginalPeriode);
        } else {
            doLagreVedtakSVP(vedtak, behandling, innvilgetPeriode, orginalPeriode);
        }
        LOG.info("Utgående feed-hendelse utgang for vedtak {}", vedtak.getId());
    }

    private void doLagreVedtakFP(BehandlingVedtak vedtak, Behandling behandling, Optional<LocalDateInterval> innvilgetPeriode,
            Optional<LocalDateInterval> orginalPeriode) {
        var fpVedtakUtgåendeHendelseBuilder = FpVedtakUtgåendeHendelse.builder();
        fpVedtakUtgåendeHendelseBuilder.aktørId(behandling.getAktørId().getId());

        var meldingstype = mapMeldingstypeFp(innvilgetPeriode, orginalPeriode);

        if (meldingstype == null) {
            // ingen endring i perioder
            return;
        }
        fpVedtakUtgåendeHendelseBuilder.type(meldingstype.getType());
        fpVedtakUtgåendeHendelseBuilder.kildeId(VEDTAK_PREFIX + vedtak.getId());

        var innhold = mapVedtakTilInnholdFp(behandling, meldingstype, innvilgetPeriode.orElseGet(orginalPeriode::get));

        var payloadJason = StandardJsonConfig.toJson(innhold);
        fpVedtakUtgåendeHendelseBuilder.payload(payloadJason);

        feedRepository.lagre(fpVedtakUtgåendeHendelseBuilder.build());
    }

    private void doLagreVedtakSVP(BehandlingVedtak vedtak, Behandling behandling, Optional<LocalDateInterval> innvilgetPeriode,
            Optional<LocalDateInterval> orginalPeriode) {
        var svpVedtakUtgåendeHendelseBuilder = SvpVedtakUtgåendeHendelse.builder();
        svpVedtakUtgåendeHendelseBuilder.aktørId(behandling.getAktørId().getId());

        var meldingstype = mapMeldingstypeSVP(innvilgetPeriode, orginalPeriode);

        if (meldingstype == null) {
            // ingen endring i perioder
            return;
        }

        svpVedtakUtgåendeHendelseBuilder.type(meldingstype.getType());
        svpVedtakUtgåendeHendelseBuilder.kildeId(VEDTAK_PREFIX + vedtak.getId());

        var innhold = mapVedtakTilInnholdSVP(behandling, meldingstype, innvilgetPeriode.orElseGet(orginalPeriode::get));

        var payloadJason = StandardJsonConfig.toJson(innhold);
        svpVedtakUtgåendeHendelseBuilder.payload(payloadJason);

        feedRepository.lagre(svpVedtakUtgåendeHendelseBuilder.build());
    }

    private static Meldingstype mapMeldingstypeFp(Optional<LocalDateInterval> innvilgetPeriode, Optional<LocalDateInterval> orginalPeriode) {
        if (innvilgetPeriode.isPresent() && orginalPeriode.isEmpty()) {
            return Meldingstype.FORELDREPENGER_INNVILGET;
        }
        if (innvilgetPeriode.isEmpty()) {
            return Meldingstype.FORELDREPENGER_OPPHOERT;
        }
        if (!innvilgetPeriode.map(ip -> orginalPeriode.map(ip::equals).orElse(false)).orElse(false)) {
            return Meldingstype.FORELDREPENGER_ENDRET;
        }
        // revurdering, men ingen endring i utbetalingsperiode
        return null;
    }

    private static Meldingstype mapMeldingstypeSVP(Optional<LocalDateInterval> innvilgetPeriode, Optional<LocalDateInterval> orginalPeriode) {
        if (innvilgetPeriode.isPresent() && orginalPeriode.isEmpty()) {
            return Meldingstype.SVANGERSKAPSPENGER_INNVILGET;
        }
        if (innvilgetPeriode.isEmpty()) {
            return Meldingstype.SVANGERSKAPSPENGER_OPPHOERT;
        }
        if (!innvilgetPeriode.map(ip -> orginalPeriode.map(ip::equals).orElse(false)).orElse(false)) {
            return Meldingstype.SVANGERSKAPSPENGER_ENDRET;
        }
        // revurdering, men ingen endring i utbetalingsperiode
        return null;
    }

    private Optional<LocalDateInterval> finnPeriode(Long behandlingId) {
        var førsteUtbetDato = finnMinsteUtbetDato(behandlingId);
        var sisteUtbetDato = finnSisteUtbetDato(behandlingId);

        if (førsteUtbetDato.isPresent() && sisteUtbetDato.isPresent()) {
            return Optional.of(new LocalDateInterval(førsteUtbetDato.get(), sisteUtbetDato.get()));
        }
        return Optional.empty();
    }

    private static Innhold mapVedtakTilInnholdFp(Behandling behandling, Meldingstype meldingstype, LocalDateInterval utbetPeriode) {
        Innhold innhold;

        if (Meldingstype.FORELDREPENGER_INNVILGET.equals(meldingstype)) {
            innhold = new ForeldrepengerInnvilget();
        } else if (Meldingstype.FORELDREPENGER_OPPHOERT.equals(meldingstype)) {
            innhold = new ForeldrepengerOpphoert();
        } else
            innhold = new ForeldrepengerEndret();

        innhold.setFoersteStoenadsdag(utbetPeriode.getFomDato());
        innhold.setSisteStoenadsdag(utbetPeriode.getTomDato());
        innhold.setAktoerId(behandling.getAktørId().getId());
        innhold.setGsakId(behandling.getFagsak().getSaksnummer().getVerdi());

        return innhold;
    }

    private static Innhold mapVedtakTilInnholdSVP(Behandling behandling, Meldingstype meldingstype, LocalDateInterval utbetPeriode) {
        Innhold innhold;

        if (Meldingstype.SVANGERSKAPSPENGER_INNVILGET.equals(meldingstype)) {
            innhold = new SvangerskapspengerInnvilget();
        } else if (Meldingstype.SVANGERSKAPSPENGER_OPPHOERT.equals(meldingstype)) {
            innhold = new SvangerskapspengerOpphoert();
        } else
            innhold = new SvangerskapspengerEndret();

        innhold.setFoersteStoenadsdag(utbetPeriode.getFomDato());
        innhold.setSisteStoenadsdag(utbetPeriode.getTomDato());
        innhold.setAktoerId(behandling.getAktørId().getId());
        innhold.setGsakId(behandling.getFagsak().getSaksnummer().getVerdi());

        return innhold;
    }

    private Optional<LocalDate> finnMinsteUtbetDato(Long behandlingId) {
        var berResultat = beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId);

        return berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
                .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
                .min(Comparator.naturalOrder())
                .map(VirkedagUtil::fomVirkedag);
    }

    private Optional<LocalDate> finnSisteUtbetDato(Long behandlingId) {
        var berResultat = beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId);
        return berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
                .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
                .max(Comparator.naturalOrder())
                .map(VirkedagUtil::tomVirkedag);
    }

    private boolean hendelseEksistererAllerede(BehandlingVedtak vedtak) {
        return feedRepository.harHendelseMedKildeId(VEDTAK_PREFIX + vedtak.getId());
    }
}
