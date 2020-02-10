package no.nav.foreldrepenger.jsonfeed.svp;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

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
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.feed.FeedRepository;
import no.nav.foreldrepenger.domene.feed.SvpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.jsonfeed.AbstractHendelsePublisererTjeneste;
import no.nav.foreldrepenger.jsonfeed.HendelsePublisererFeil;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerOpphoert;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Innhold;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Meldingstype;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.SvangerskapspengerEndret;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.SvangerskapspengerInnvilget;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.SvangerskapspengerOpphoert;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class HendelsePublisererTjenesteImpl extends AbstractHendelsePublisererTjeneste {
    private static final Logger log = LoggerFactory.getLogger(HendelsePublisererTjenesteImpl.class);

    private FeedRepository feedRepository;
    private SvangerskapspengerUttakResultatRepository uttakResultatRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    protected static class UttakFomTom {
        LocalDate førsteStønadsdag;
        LocalDate sisteStønadsdag;
    }

    public HendelsePublisererTjenesteImpl() {
        // CDI
    }

    @Inject
    public HendelsePublisererTjenesteImpl(FeedRepository feedRepository,
                                          SvangerskapspengerUttakResultatRepository uttakResultatRepository,
                                          BehandlingRepositoryProvider repositoryProvider,
                                          BehandlingsresultatRepository behandlingsresultatRepository,
                                          EtterkontrollRepository etterkontrollRepository) {
        super(behandlingsresultatRepository, etterkontrollRepository);
        this.feedRepository = feedRepository;
        this.uttakResultatRepository = uttakResultatRepository;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    protected void doLagreVedtak(BehandlingVedtak vedtak, BehandlingType behandlingType) {
        Innhold innhold = mapVedtakTilInnhold(vedtak);

        Meldingstype meldingstype;

        if (erFørstegangsSøknad(behandlingType) || erInnvilgetRevurdering(behandlingType, vedtak.getBehandlingsresultat().getBehandlingResultatType())) {
            meldingstype = Meldingstype.SVANGERSKAPSPENGER_INNVILGET;
        } else if (erOpphørtRevurdering(behandlingType, vedtak.getBehandlingsresultat().getBehandlingResultatType())) {
            meldingstype = Meldingstype.SVANGERSKAPSPENGER_OPPHOERT;
        } else {
            meldingstype = Meldingstype.SVANGERSKAPSPENGER_ENDRET;
        }

        String payloadJason = JsonMapper.toJson(innhold);

        SvpVedtakUtgåendeHendelse svpVedtakUtgåendeHendelse = SvpVedtakUtgåendeHendelse.builder()
            .aktørId(vedtak.getBehandlingsresultat().getBehandling().getAktørId().getId())
            .payload(payloadJason)
            .type(meldingstype.getType())
            .kildeId(VEDTAK_PREFIX + vedtak.getId())
            .build();
        feedRepository.lagre(svpVedtakUtgåendeHendelse);
    }

    @Override
    protected boolean hendelseEksistererAllerede(BehandlingVedtak vedtak) {
        return feedRepository.harHendelseMedKildeId(SvpVedtakUtgåendeHendelse.class, VEDTAK_PREFIX + vedtak.getId());
    }

    @Override
    public void lagreFagsakAvsluttet(FagsakStatusEvent event) {
        log.info("lagrer utgående hendelse for fagsak {}", event.getFagsakId());
        Innhold innhold = mapFagsakEventTilInnhold(event);

        String payloadJason = JsonMapper.toJson(innhold);

        SvpVedtakUtgåendeHendelse svpVedtakUtgåendeHendelse = SvpVedtakUtgåendeHendelse.builder()
            .aktørId(event.getAktørId().getId())
            .payload(payloadJason)
            .type(Meldingstype.SVANGERSKAPSPENGER_OPPHOERT.getType())
            .kildeId(FAGSAK_PREFIX + event.getFagsakId())
            .build();
        feedRepository.lagre(svpVedtakUtgåendeHendelse);
    }

    @Override
    protected boolean uttakFomEllerTomErEndret(Optional<Behandlingsresultat> gammeltResultat, Behandlingsresultat nyttResultat) {
        Optional<SvangerskapspengerUttakResultatEntitet> gammeltUttakResultat = gammeltResultat.isPresent()
            ? uttakResultatRepository.hentHvisEksisterer(gammeltResultat.get().getBehandling().getId())
            : Optional.empty();
        Optional<SvangerskapspengerUttakResultatEntitet> nyttUttakResultat = uttakResultatRepository.hentHvisEksisterer(nyttResultat.getBehandling().getId());

        if (!gammeltUttakResultat.isPresent() || !nyttUttakResultat.isPresent()) {
            return true;
        }
        UttakFomTom gammelStønadsperiode = finnFørsteOgSisteStønadsdag(gammeltUttakResultat.get());
        UttakFomTom nyStønadsperionde = finnFørsteOgSisteStønadsdag(nyttUttakResultat.get());

        return !Objects.equals(gammelStønadsperiode.førsteStønadsdag, nyStønadsperionde.førsteStønadsdag) ||
            !Objects.equals(gammelStønadsperiode.sisteStønadsdag, nyStønadsperionde.sisteStønadsdag);
    }

    private Innhold mapFagsakEventTilInnhold(FagsakStatusEvent event) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(event.getFagsakId());
        Innhold innhold = new ForeldrepengerOpphoert();
        innhold.setGsakId(fagsak.getSaksnummer().getVerdi());
        innhold.setAktoerId(event.getAktørId().getId());

        Optional<Behandling> behandling = behandlingRepository.hentSisteBehandlingForFagsakId(event.getFagsakId());
        if (!behandling.isPresent()) {
            throw HendelsePublisererFeil.FACTORY.finnerIkkeBehandlingForFagsak().toException();
        }
        Optional<SvangerskapspengerUttakResultatEntitet> uttakResultat = uttakResultatRepository.hentHvisEksisterer(behandling.get().getId());
        if (!uttakResultat.isPresent()) {
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
            innhold = new SvangerskapspengerInnvilget();
        } else if (erOpphørtRevurdering(behandlingType, vedtak.getBehandlingsresultat().getBehandlingResultatType())) {
            innhold = new SvangerskapspengerOpphoert();
            originalBehandling = vedtak.getBehandlingsresultat().getBehandling().getOriginalBehandling();
        } else {
            innhold = new SvangerskapspengerEndret();
            originalBehandling = vedtak.getBehandlingsresultat().getBehandling().getOriginalBehandling();
        }

        if (originalBehandling.isPresent() && !uttakFomEllerTomErEndret(hentBehandlingsresultat(originalBehandling.get()), vedtak.getBehandlingsresultat())) { // NOSONAR
            throw HendelsePublisererFeil.FACTORY.fantIngenEndringIUttakFomEllerTom().toException();
        }

        Optional<SvangerskapspengerUttakResultatEntitet> uttakResultat = uttakResultatRepository
            .hentHvisEksisterer(vedtak.getBehandlingsresultat().getBehandling().getId());
        UttakFomTom uttakFomTom;
        if (uttakResultat.isPresent()) {
            uttakFomTom = finnFørsteOgSisteStønadsdag(uttakResultat.get());
        } else if (originalBehandling.isPresent()) {
            Optional<SvangerskapspengerUttakResultatEntitet> origUttakResultat = uttakResultatRepository.hentHvisEksisterer(originalBehandling.get().getId());
            if (!origUttakResultat.isPresent()) {
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

    private UttakFomTom finnFørsteOgSisteStønadsdag(SvangerskapspengerUttakResultatEntitet uttakResultat) {
        UttakFomTom resultat = new UttakFomTom();

        if (uttakResultat.finnFørsteInnvilgedeUttaksdatoMedUtbetalingsgrad().isPresent()) {
            resultat.førsteStønadsdag = uttakResultat.finnFørsteInnvilgedeUttaksdatoMedUtbetalingsgrad().get();
        }
        if (uttakResultat.finnSisteInnvilgedeUttaksdatoMedUtbetalingsgrad().isPresent()) {
            resultat.sisteStønadsdag = uttakResultat.finnSisteInnvilgedeUttaksdatoMedUtbetalingsgrad().get();
        }
        return resultat;
    }

    private UttakFomTom finnOriginalStønadsOppstart(SvangerskapspengerUttakResultatEntitet uttakResultat) {
        UttakFomTom resultat = new UttakFomTom();
        if (uttakResultat.finnFørsteUttaksdato().isPresent()) {
            resultat.førsteStønadsdag = uttakResultat.finnFørsteUttaksdato().get();
            resultat.sisteStønadsdag = resultat.førsteStønadsdag;
        }
        return resultat;
    }
}
