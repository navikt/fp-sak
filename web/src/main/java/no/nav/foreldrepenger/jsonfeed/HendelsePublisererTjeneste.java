package no.nav.foreldrepenger.jsonfeed;

import java.util.Collections;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.feed.FeedRepository;
import no.nav.foreldrepenger.domene.feed.FpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.feed.SvpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerEndret;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerInnvilget;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerOpphoert;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Innhold;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Meldingstype;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.SvangerskapspengerEndret;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.SvangerskapspengerInnvilget;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.SvangerskapspengerOpphoert;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@ApplicationScoped
public class HendelsePublisererTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(HendelsePublisererTjeneste.class);
    private static final String VEDTAK_PREFIX = "VT";

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FeedRepository feedRepository;
    private PersoninfoAdapter personinfoAdapter;

    public HendelsePublisererTjeneste() {
        // Creatively Diversified Investments
    }

    @Inject
    public HendelsePublisererTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                      FeedRepository feedRepository,
                                      PersoninfoAdapter personinfoAdapter) {
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = behandlingRepositoryProvider.getBeregningsresultatRepository();
        this.feedRepository = feedRepository;
        this.personinfoAdapter = personinfoAdapter;
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

        var fnr = personinfoAdapter.hentFnr(behandling.getAktørId()).orElseThrow();

        LOG.info("Utgående feed-hendelse vedtakId {} funnet perioder", vedtak.getId());

        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            doLagreVedtakFP(vedtak, behandling, fnr, innvilgetPeriode, orginalPeriode);
        } else {
            doLagreVedtakSVP(vedtak, behandling, fnr, innvilgetPeriode, orginalPeriode);
        }
        LOG.info("Utgående feed-hendelse utgang for vedtak {}", vedtak.getId());
    }

    private void doLagreVedtakFP(BehandlingVedtak vedtak, Behandling behandling, PersonIdent fnr, Optional<LocalDateInterval> innvilgetPeriode,
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

        var innhold = mapVedtakTilInnholdFp(behandling, fnr, meldingstype, innvilgetPeriode.orElseGet(orginalPeriode::get));

        var payloadJason = StandardJsonConfig.toJson(innhold);
        fpVedtakUtgåendeHendelseBuilder.payload(payloadJason);

        feedRepository.lagre(fpVedtakUtgåendeHendelseBuilder.build());
    }

    private void doLagreVedtakSVP(BehandlingVedtak vedtak, Behandling behandling, PersonIdent fnr, Optional<LocalDateInterval> innvilgetPeriode,
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

        var innhold = mapVedtakTilInnholdSVP(behandling, fnr, meldingstype, innvilgetPeriode.orElseGet(orginalPeriode::get));

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
        var tilkjentYtelseTidslinje = finnTilkjentYtelseTidlinje(behandlingId);
        if (tilkjentYtelseTidslinje.isEmpty()) {
            return Optional.empty();
        }
        var førsteUtbetDato = VirkedagUtil.fomVirkedag(tilkjentYtelseTidslinje.getMinLocalDate());
        var sisteUtbetDato = VirkedagUtil.tomVirkedag(tilkjentYtelseTidslinje.getMaxLocalDate());

        //bruken av VikedagUtil vil gjøre at hvis hele tidslinjen består av kun en helg, blir førsteUtbetDato etter sisteUtbetDato
        var bareTilkjentYtelseIHelg = førsteUtbetDato.isAfter(sisteUtbetDato);
        if (bareTilkjentYtelseIHelg) {
            return Optional.empty();
        }
        return Optional.of(new LocalDateInterval(førsteUtbetDato, sisteUtbetDato));
    }

    private static Innhold mapVedtakTilInnholdFp(Behandling behandling, PersonIdent fnr, Meldingstype meldingstype, LocalDateInterval utbetPeriode) {
        Innhold innhold;

        if (Meldingstype.FORELDREPENGER_INNVILGET.equals(meldingstype)) {
            innhold = new ForeldrepengerInnvilget();
        } else if (Meldingstype.FORELDREPENGER_OPPHOERT.equals(meldingstype)) {
            innhold = new ForeldrepengerOpphoert();
        } else {
            innhold = new ForeldrepengerEndret();
        }

        innhold.setFoersteStoenadsdag(utbetPeriode.getFomDato());
        innhold.setSisteStoenadsdag(utbetPeriode.getTomDato());
        innhold.setAktoerId(behandling.getAktørId().getId());
        innhold.setGsakId(behandling.getSaksnummer().getVerdi());
        innhold.setFnr(fnr.getIdent());

        return innhold;
    }

    private static Innhold mapVedtakTilInnholdSVP(Behandling behandling, PersonIdent fnr, Meldingstype meldingstype, LocalDateInterval utbetPeriode) {
        Innhold innhold;

        if (Meldingstype.SVANGERSKAPSPENGER_INNVILGET.equals(meldingstype)) {
            innhold = new SvangerskapspengerInnvilget();
        } else if (Meldingstype.SVANGERSKAPSPENGER_OPPHOERT.equals(meldingstype)) {
            innhold = new SvangerskapspengerOpphoert();
        } else {
            innhold = new SvangerskapspengerEndret();
        }

        innhold.setFoersteStoenadsdag(utbetPeriode.getFomDato());
        innhold.setSisteStoenadsdag(utbetPeriode.getTomDato());
        innhold.setAktoerId(behandling.getAktørId().getId());
        innhold.setGsakId(behandling.getSaksnummer().getVerdi());
        innhold.setFnr(fnr.getIdent());

        return innhold;
    }

    private LocalDateTimeline<Boolean> finnTilkjentYtelseTidlinje(Long behandlingId) {
        var brResultat = beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId);

        var brPerioder = brResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList());
        var segmenterFinnesYtelse = brPerioder.stream()
            .filter(brPeriode -> brPeriode.getDagsats() > 0)
            .map(brPeriode -> new LocalDateSegment<>(brPeriode.getBeregningsresultatPeriodeFom(), brPeriode.getBeregningsresultatPeriodeTom(), true))
            .sorted()
            .toList();
        return new LocalDateTimeline<>(segmenterFinnesYtelse, StandardCombinators::alwaysTrueForMatch);
    }

    private boolean hendelseEksistererAllerede(BehandlingVedtak vedtak) {
        return feedRepository.harHendelseMedKildeId(VEDTAK_PREFIX + vedtak.getId());
    }
}
