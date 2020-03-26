package no.nav.foreldrepenger.jsonfeed.fp;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FagsakStatusEvent;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.feed.FeedRepository;
import no.nav.foreldrepenger.domene.feed.FpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.jsonfeed.AbstractHendelsePublisererTjeneste;
import no.nav.foreldrepenger.jsonfeed.HendelsePublisererFeil;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerEndret;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerInnvilget;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerOpphoert;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Innhold;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Meldingstype;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class HendelsePublisererTjenesteImpl extends AbstractHendelsePublisererTjeneste {
    private static final Logger log = LoggerFactory.getLogger(HendelsePublisererTjenesteImpl.class);

    private FeedRepository feedRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    protected static class UttakFomTom {
        LocalDate førsteStønadsdag;

        public LocalDate getFørsteStønadsdag() {
            return førsteStønadsdag;
        }

        public void setFørsteStønadsdag(LocalDate førsteStønadsdag) {
            this.førsteStønadsdag = førsteStønadsdag;
        }

        LocalDate sisteStønadsdag;
    }

    public HendelsePublisererTjenesteImpl() {
        // CDI
    }

    @Inject
    public HendelsePublisererTjenesteImpl(FeedRepository feedRepository,
                                          ForeldrepengerUttakTjeneste uttakTjeneste,
                                          BehandlingRepositoryProvider repositoryProvider,
                                          BehandlingsresultatRepository behandlingsresultatRepository,
                                          EtterkontrollRepository etterkontrollRepository) {
        super(behandlingsresultatRepository, etterkontrollRepository);
        this.feedRepository = feedRepository;
        this.uttakTjeneste = uttakTjeneste;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    protected void doLagreVedtak(BehandlingVedtak vedtak, BehandlingType behandlingType) {
        Innhold innhold = mapVedtakTilInnhold(vedtak);

        Meldingstype meldingstype;

        if (erFørstegangsSøknad(behandlingType) || erInnvilgetRevurdering(behandlingType, vedtak.getBehandlingsresultat().getBehandlingResultatType())) {
            meldingstype = Meldingstype.FORELDREPENGER_INNVILGET;
        } else if (erOpphørtRevurdering(behandlingType, vedtak.getBehandlingsresultat().getBehandlingResultatType())) {
            meldingstype = Meldingstype.FORELDREPENGER_OPPHOERT;
        } else {
            meldingstype = Meldingstype.FORELDREPENGER_ENDRET;
        }

        String payloadJason = JsonMapper.toJson(innhold);

        FpVedtakUtgåendeHendelse fpVedtakUtgåendeHendelse = FpVedtakUtgåendeHendelse.builder()
            .aktørId(vedtak.getBehandlingsresultat().getBehandling().getAktørId().getId())
            .payload(payloadJason)
            .type(meldingstype.getType())
            .kildeId(VEDTAK_PREFIX + vedtak.getId())
            .build();
        feedRepository.lagre(fpVedtakUtgåendeHendelse);
    }

    @Override
    protected boolean hendelseEksistererAllerede(BehandlingVedtak vedtak) {
        return feedRepository.harHendelseMedKildeId(FpVedtakUtgåendeHendelse.class, VEDTAK_PREFIX + vedtak.getId());
    }

    @Override
    public void lagreFagsakAvsluttet(FagsakStatusEvent event) {
        log.info("lagrer utgående hendelse for fagsak {}", event.getFagsakId());
        Innhold innhold = mapFagsakEventTilInnhold(event);

        String payloadJason = JsonMapper.toJson(innhold);

        FpVedtakUtgåendeHendelse fpVedtakUtgåendeHendelse = FpVedtakUtgåendeHendelse.builder()
            .aktørId(event.getAktørId().getId())
            .payload(payloadJason)
            .type(Meldingstype.FORELDREPENGER_OPPHOERT.getType())
            .kildeId(FAGSAK_PREFIX + event.getFagsakId())
            .build();
        feedRepository.lagre(fpVedtakUtgåendeHendelse);
    }

    @Override
    protected boolean uttakFomEllerTomErEndret(Optional<Behandlingsresultat> gammeltResultat, Behandlingsresultat nyttResultat) {
        Optional<ForeldrepengerUttak> gammeltUttakResultat = gammeltResultat.isPresent()
            ? uttakTjeneste.hentUttakHvisEksisterer(gammeltResultat.get().getBehandling().getId())
            : Optional.empty();
        var nyttUttakResultat = uttakTjeneste.hentUttakHvisEksisterer(nyttResultat.getBehandling().getId());

        if (gammeltUttakResultat.isEmpty() || nyttUttakResultat.isEmpty()) {
            return true;
        }
        UttakFomTom gammelStønadsperiode = finnFørsteOgSisteStønadsdag(gammeltUttakResultat.get());
        UttakFomTom nyStønadsperionde = finnFørsteOgSisteStønadsdag(nyttUttakResultat.get());

        return !gammelStønadsperiode.getFørsteStønadsdag().equals(nyStønadsperionde.førsteStønadsdag) ||
            !gammelStønadsperiode.sisteStønadsdag.equals(nyStønadsperionde.sisteStønadsdag);
    }

    private Innhold mapFagsakEventTilInnhold(FagsakStatusEvent event) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(event.getFagsakId());
        Innhold innhold = new ForeldrepengerOpphoert();
        innhold.setGsakId(fagsak.getSaksnummer().getVerdi());
        innhold.setAktoerId(event.getAktørId().getId());

        Optional<Behandling> behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(event.getFagsakId());
        if (behandling.isEmpty()) {
            throw HendelsePublisererFeil.FACTORY.finnerIkkeBehandlingForFagsak().toException();
        }
        var uttakResultat = uttakTjeneste.hentUttakHvisEksisterer(behandling.get().getId());
        if (uttakResultat.isEmpty()) {
            throw HendelsePublisererFeil.FACTORY.finnerIkkeRelevantUttaksplanForVedtak().toException();
        }

        UttakFomTom uttakFomTom = finnFørsteOgSisteStønadsdag(uttakResultat.get());
        innhold.setFoersteStoenadsdag(uttakFomTom.førsteStønadsdag);
        innhold.setSisteStoenadsdag(uttakFomTom.sisteStønadsdag);

        return innhold;
    }

    private Innhold mapVedtakTilInnhold(BehandlingVedtak vedtak) {
        Innhold innhold;
        BehandlingType behandlingType = vedtak.getBehandlingsresultat().getBehandling().getType();
        Optional<Behandling> originalBehandling = Optional.empty();
        if (erFørstegangsSøknad(behandlingType) || erInnvilgetRevurdering(behandlingType, vedtak.getBehandlingsresultat().getBehandlingResultatType())) {
            innhold = new ForeldrepengerInnvilget();
        } else if (erOpphørtRevurdering(behandlingType, vedtak.getBehandlingsresultat().getBehandlingResultatType())) {
            innhold = new ForeldrepengerOpphoert();
            originalBehandling = vedtak.getBehandlingsresultat().getBehandling().getOriginalBehandling();
        } else {
            innhold = new ForeldrepengerEndret();
            originalBehandling = vedtak.getBehandlingsresultat().getBehandling().getOriginalBehandling();
        }

        if (originalBehandling.isPresent() && !uttakFomEllerTomErEndret(hentBehandlingsresultat(originalBehandling.get()), vedtak.getBehandlingsresultat())) {// NOSONAR
            throw HendelsePublisererFeil.FACTORY.fantIngenEndringIUttakFomEllerTom().toException();
        }

        var uttakResultat = uttakTjeneste.hentUttakHvisEksisterer(vedtak.getBehandlingsresultat().getBehandling().getId());
        UttakFomTom uttakFomTom;
        if (uttakResultat.isPresent()) {
            uttakFomTom = finnFørsteOgSisteStønadsdag(uttakResultat.get());
        } else if (originalBehandling.isPresent()) {

            var origUttakResultat = uttakTjeneste.hentUttakHvisEksisterer(originalBehandling.get().getId());
            if (origUttakResultat.isEmpty()) {
                throw HendelsePublisererFeil.FACTORY.finnerIkkeRelevantUttaksplanForVedtak().toException();
            }
            uttakFomTom = finnOriginalStønadsOppstart(origUttakResultat.get());
        } else {
            throw HendelsePublisererFeil.FACTORY.finnerIkkeRelevantUttaksplanForVedtak().toException();
        }

        innhold.setFoersteStoenadsdag(uttakFomTom.førsteStønadsdag);
        innhold.setSisteStoenadsdag(uttakFomTom.sisteStønadsdag);
        innhold.setAktoerId(vedtak.getBehandlingsresultat().getBehandling().getAktørId().getId());
        innhold.setGsakId(vedtak.getBehandlingsresultat().getBehandling().getFagsak().getSaksnummer().getVerdi());

        return innhold;
    }

    private UttakFomTom finnFørsteOgSisteStønadsdag(ForeldrepengerUttak uttak) {
        UttakFomTom resultat = new UttakFomTom();
        var innvilgedePerioder = uttak.getGjeldendePerioder().stream()
            .filter(ForeldrepengerUttakPeriode::isInnvilget)
            .collect(Collectors.toList());
        if (!innvilgedePerioder.isEmpty()) {
            resultat.førsteStønadsdag = innvilgedePerioder.stream().map(ForeldrepengerUttakPeriode::getFom).min(LocalDate::compareTo).get();
            resultat.sisteStønadsdag = innvilgedePerioder.stream().map(ForeldrepengerUttakPeriode::getTom).max(LocalDate::compareTo).get();
        } else {
            resultat.førsteStønadsdag = uttak.getGjeldendePerioder().stream().map(ForeldrepengerUttakPeriode::getFom)
                .min(LocalDate::compareTo).get();

            // Hvis det ikke finnes noen innvilgede perioder etter revurdering så vil hendelsen være at ytelsen opphører samme dag som den opprinnelig
            // ble innvilget
            resultat.sisteStønadsdag = resultat.førsteStønadsdag;
        }
        return resultat;
    }

    private UttakFomTom finnOriginalStønadsOppstart(ForeldrepengerUttak uttak) {
        UttakFomTom resultat = new UttakFomTom();
        var perioder = uttak.getGjeldendePerioder().stream()
            .filter(ForeldrepengerUttakPeriode::isInnvilget)
            .collect(Collectors.toList());
        if (perioder.isEmpty()) {
            perioder = uttak.getGjeldendePerioder();
        }
        resultat.førsteStønadsdag = perioder.stream().map(ForeldrepengerUttakPeriode::getFom).min(LocalDate::compareTo).get();
        resultat.sisteStønadsdag = resultat.førsteStønadsdag;

        return resultat;
    }
}
