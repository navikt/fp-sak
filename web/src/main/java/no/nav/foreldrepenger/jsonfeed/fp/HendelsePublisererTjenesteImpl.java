package no.nav.foreldrepenger.jsonfeed.fp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FagsakStatusEvent;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
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
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.feed.FeedRepository;
import no.nav.foreldrepenger.domene.feed.FpVedtakUtgåendeHendelse;
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
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;


    public HendelsePublisererTjenesteImpl() {
        // CDI
    }

    @Inject
    public HendelsePublisererTjenesteImpl(FeedRepository feedRepository,
                                          BehandlingRepositoryProvider repositoryProvider,
                                          BehandlingsresultatRepository behandlingsresultatRepository,
                                          EtterkontrollRepository etterkontrollRepository) {
        super(behandlingsresultatRepository, etterkontrollRepository, repositoryProvider);
        this.feedRepository = feedRepository;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    @Override
    protected void doLagreVedtak(BehandlingVedtak vedtak, Behandling behandling) {
        Innhold innhold = mapVedtakTilInnhold(vedtak, behandling);

        Meldingstype meldingstype;
        BehandlingType behandlingType = behandling.getType();

        if (erFørstegangsSøknad(behandlingType) || erInnvilgetRevurdering(behandlingType, vedtak.getBehandlingsresultat().getBehandlingResultatType())) {
            meldingstype = Meldingstype.FORELDREPENGER_INNVILGET;
        } else if (erOpphørtRevurdering(behandlingType, vedtak.getBehandlingsresultat().getBehandlingResultatType())) {
            meldingstype = Meldingstype.FORELDREPENGER_OPPHOERT;
        } else {
            meldingstype = Meldingstype.FORELDREPENGER_ENDRET;
        }

        String payloadJason = JsonMapper.toJson(innhold);

        FpVedtakUtgåendeHendelse fpVedtakUtgåendeHendelse = FpVedtakUtgåendeHendelse.builder()
            .aktørId(behandling.getAktørId().getId())
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
    protected boolean uttakFomEllerTomErEndret(Long orginalbehId, Long behandlingId) {
        Optional<LocalDate> førsteUtbetDatoOrgBeh = finnMinsteUtbetDato(orginalbehId);
        Optional<LocalDate> førsteUtbetDato = finnMinsteUtbetDato(behandlingId);

        if (førsteUtbetDatoOrgBeh.isEmpty() || førsteUtbetDato.isEmpty()) {
            return true;
        }

        Optional<LocalDate> sisteUtbetDatoOrgBeh = finnSisteUtbetDato(orginalbehId);
        Optional<LocalDate> sisteUtbetDato = finnSisteUtbetDato(behandlingId);

        // Hvis det ikke finnes noen perioder etter revurdering så vil hendelsen være at ytelsen opphører samme dag som den opprinnelig ble innvilget
        if (sisteUtbetDato.isEmpty() ) {
                sisteUtbetDato = førsteUtbetDato;
            }
        if (sisteUtbetDatoOrgBeh.isEmpty()) {
            sisteUtbetDatoOrgBeh = førsteUtbetDatoOrgBeh;
        }

        return !førsteUtbetDatoOrgBeh.get().equals(førsteUtbetDato.get()) ||
            !sisteUtbetDatoOrgBeh.get().equals(sisteUtbetDato.get());
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

        Optional<LocalDate> førsteUtbetDato = finnMinsteUtbetDato(behandling.get().getId());
        if (førsteUtbetDato.isEmpty()) {
            throw HendelsePublisererFeil.FACTORY.finnerIkkeRelevantUttaksplanForVedtak().toException();
        }

        Optional<LocalDate> sisteUtbetDato = finnSisteUtbetDato(behandling.get().getId());
        // Hvis det ikke finnes noen innvilgede perioder etter revurdering så vil hendelsen være at ytelsen opphører samme dag som den opprinnelig ble innvilget
        if (sisteUtbetDato.isEmpty()) {
            sisteUtbetDato = førsteUtbetDato;
        }

        førsteUtbetDato.ifPresent(innhold::setFoersteStoenadsdag);
        sisteUtbetDato.ifPresent(innhold::setSisteStoenadsdag);

        return innhold;
    }

    private Innhold mapVedtakTilInnhold(BehandlingVedtak vedtak, Behandling behandling) {
        Innhold innhold;
        BehandlingType behandlingType = behandling.getType();
        Optional<Behandling> originalBehandling = Optional.empty();

        if (erFørstegangsSøknad(behandlingType) || erInnvilgetRevurdering(behandlingType, vedtak.getBehandlingsresultat().getBehandlingResultatType())) {
            innhold = new ForeldrepengerInnvilget();
        } else if (erOpphørtRevurdering(behandlingType, vedtak.getBehandlingsresultat().getBehandlingResultatType())) {
            innhold = new ForeldrepengerOpphoert();
            originalBehandling = behandling.getOriginalBehandling();
        } else {
            innhold = new ForeldrepengerEndret();
            originalBehandling = behandling.getOriginalBehandling();
        }

        if (originalBehandling.isPresent() && !uttakFomEllerTomErEndret(originalBehandling.get().getId(), behandling.getId())) {// NOSONAR
            throw HendelsePublisererFeil.FACTORY.fantIngenEndringIUttakFomEllerTom().toException();
        }

        Optional<LocalDate> førsteUtbetDato = finnMinsteUtbetDato(behandling.getId());
        Optional<LocalDate> sisteUtbetDato = finnSisteUtbetDato(behandling.getId());

        if (førsteUtbetDato.isEmpty()) {
            if (originalBehandling.isPresent()){
                førsteUtbetDato = finnMinsteUtbetDato(originalBehandling.get().getId());
                sisteUtbetDato = førsteUtbetDato;
            }
            //finner ingen minste dato, noe er feil
            if (førsteUtbetDato.isEmpty()) {
                throw HendelsePublisererFeil.FACTORY.finnerIkkeRelevantUttaksplanForVedtak().toException();
            }
        }
        // Hvis det ikke finnes noen innvilgede perioder etter revurdering så vil hendelsen være at ytelsen opphører samme dag som den opprinnelig ble innvilget
        if (sisteUtbetDato.isEmpty()) {
            sisteUtbetDato = førsteUtbetDato;
        }

        førsteUtbetDato.ifPresent(innhold::setFoersteStoenadsdag);
        sisteUtbetDato.ifPresent(innhold::setSisteStoenadsdag);
        innhold.setAktoerId(behandling.getAktørId().getId());
        innhold.setGsakId(behandling.getFagsak().getSaksnummer().getVerdi());

        return innhold;
    }


    private Optional<LocalDate> finnMinsteUtbetDato(Long behandlingId) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);

        if (henlagtEllerOpphørFomFørsteUttak(behandlingId)) {
            return berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
                .min(Comparator.naturalOrder())
                .map(this::fomMandag);
        }else {
            return berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
                .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
                .min(Comparator.naturalOrder())
                .map(this::fomMandag);
        }
    }
    private boolean henlagtEllerOpphørFomFørsteUttak(Long behandlingId) {
        var resultat = behandlingsresultatRepository.hentHvisEksisterer(behandlingId).map(Behandlingsresultat::getBehandlingResultatType).orElse(BehandlingResultatType.INNVILGET);
        if (resultat.erHenlagt())
            return true;
        // Aktuelt for revurderinger med Opphør fom start. Enkelte har opphør fom senere dato.
        return Set.of(BehandlingResultatType.OPPHØR, BehandlingResultatType.AVSLÅTT).contains(resultat)
            && finnSisteUtbetDato(behandlingId).isEmpty();
    }
    private Optional<LocalDate> finnSisteUtbetDato(Long behandlingId) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);
        return berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder())
            .map(this::tomFredag);
    }

    private LocalDate fomMandag(LocalDate fom) {
        DayOfWeek ukedag = DayOfWeek.from(fom);
        if (DayOfWeek.SUNDAY.getValue() == ukedag.getValue())
            return fom.plusDays(1);
        if (DayOfWeek.SATURDAY.getValue() == ukedag.getValue())
            return fom.plusDays(2);
        return fom;
    }

    private LocalDate tomFredag(LocalDate tom) {
        DayOfWeek ukedag = DayOfWeek.from(tom);
        if (DayOfWeek.SUNDAY.getValue() == ukedag.getValue())
            return tom.minusDays(2);
        if (DayOfWeek.SATURDAY.getValue() == ukedag.getValue())
            return tom.minusDays(1);
        return tom;
    }
}
