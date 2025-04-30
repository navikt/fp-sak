package no.nav.foreldrepenger.behandling.steg.simulering;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.kontrakter.simulering.resultat.v1.SimuleringResultatDto;
import no.nav.foreldrepenger.produksjonsstyring.tilbakekreving.FptilbakeRestKlient;
import no.nav.foreldrepenger.økonomistøtte.SimulerOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.simulering.klient.FpOppdragRestKlient;
import no.nav.foreldrepenger.økonomistøtte.simulering.tjeneste.SimuleringIntegrasjonTjeneste;
import no.nav.vedtak.exception.IntegrasjonException;

@BehandlingStegRef(BehandlingStegType.SIMULER_OPPDRAG)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class SimulerOppdragSteg implements BehandlingSteg {

    private static final Logger LOG = LoggerFactory.getLogger(SimulerOppdragSteg.class);

    private static final int ÅPNINGSTID = 7;
    private static final int STENGETID = 19;
    private static final String COUNTER_ETTERBETALING_NAME = "foreldrepenger.etterbetaling";

    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private SimulerOppdragTjeneste simulerOppdragTjeneste;
    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private FpOppdragRestKlient fpOppdragRestKlient;
    private FptilbakeRestKlient fptilbakeRestKlient;
    private BeregningsresultatRepository beregningsresultatRepository;

    SimulerOppdragSteg() {
        // for CDI proxy
    }

    @Inject
    public SimulerOppdragSteg(BehandlingRepositoryProvider repositoryProvider,
                              BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                              SimulerOppdragTjeneste simulerOppdragTjeneste,
                              SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste,
                              TilbakekrevingRepository tilbakekrevingRepository,
                              FpOppdragRestKlient fpOppdragRestKlient,
                              FptilbakeRestKlient fptilbakeRestKlient,
                              BeregningsresultatRepository beregningsresultatRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.simulerOppdragTjeneste = simulerOppdragTjeneste;
        this.simuleringIntegrasjonTjeneste = simuleringIntegrasjonTjeneste;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.fpOppdragRestKlient = fpOppdragRestKlient;
        this.fptilbakeRestKlient = fptilbakeRestKlient;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        try {
            startSimulering(behandling);
            return utledAksjonspunkt(behandling);
        } catch (IntegrasjonException e) {
            opprettFortsettBehandlingTask(behandling);
            return BehandleStegResultat.settPåVent();
        }
    }

    @Override
    public BehandleStegResultat gjenopptaSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        startSimulering(behandling);
        return utledAksjonspunkt(behandling);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
                                   BehandlingStegType fraSteg) {
        if (!BehandlingStegType.SIMULER_OPPDRAG.equals(tilSteg)) {
            var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            fpOppdragRestKlient.kansellerSimulering(kontekst.getBehandlingId(), behandling.getUuid(), behandling.getSaksnummer().getVerdi());
            tilbakekrevingRepository.deaktiverEksisterendeTilbakekrevingValg(behandling);
            tilbakekrevingRepository.deaktiverEksisterendeTilbakekrevingInntrekk(behandling);
        }
    }

    private void startSimulering(Behandling behandling) {
        var oppdragskontroll = simulerOppdragTjeneste.hentOppdragskontrollForBehandling(behandling.getId());
        simuleringIntegrasjonTjeneste.startSimulering(oppdragskontroll, behandling.getUuid(), behandling.getSaksnummer().getVerdi());
    }

    private void opprettFortsettBehandlingTask(Behandling behandling) {
        var nesteKjøringEtter = utledNesteKjøring();
        behandlingProsesseringTjeneste.opprettTasksForFortsettBehandlingResumeStegNesteKjøring(behandling,
            BehandlingStegType.SIMULER_OPPDRAG, nesteKjøringEtter);
    }

    private BehandleStegResultat utledAksjonspunkt(Behandling behandling) {
        var aksjonspunkter = new ArrayList<AksjonspunktDefinisjon>();

        var etterbetalingskontrollResultat = harStorEtterbetalingTilSøker(behandling);
        if (etterbetalingskontrollResultat.map(EtterbetalingskontrollResultat::overstigerGrense).orElse(false)) {
            aksjonspunkter.add(AksjonspunktDefinisjon.KONTROLLER_STOR_ETTERBETALING_SØKER);
        }

        var simuleringResultatDto = simuleringIntegrasjonTjeneste.hentResultat(behandling.getId(), behandling.getUuid(), behandling.getSaksnummer().getVerdi());
        if (simuleringResultatDto.isPresent()) {
            var resultatDto = simuleringResultatDto.get();
            lagreBrukInntrekk(behandling, resultatDto);
            // vi sender TILBAKEKREVING_OPPDATER når det finnes et simulering resultat
            if (kanOppdatereEksisterendeTilbakekrevingsbehandling(behandling, resultatDto)) {
                lagreTilbakekrevingValg(behandling, TilbakekrevingValg.medOppdaterTilbakekrevingsbehandling());
            } else if (SimuleringIntegrasjonTjeneste.harFeilutbetaling(resultatDto)) {
                aksjonspunkter.add(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
            } else if (resultatDto.sumInntrekk() != null && resultatDto.sumInntrekk() != 0) {
                lagreTilbakekrevingValg(behandling, TilbakekrevingValg.medAutomatiskInntrekk());
            }
        }
        loggEtterbetalingVedFlereGrenseverdier(behandling, aksjonspunkter, etterbetalingskontrollResultat);
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
    }

    private static void loggEtterbetalingVedFlereGrenseverdier(Behandling behandling, ArrayList<AksjonspunktDefinisjon> aksjonspunkter, Optional<EtterbetalingskontrollResultat> etterbetalingskontrollResultatOpt) {
        try {
            if (etterbetalingskontrollResultatOpt.isEmpty()) {
                return;
            }

            var harAndreAksjonspunkt = Stream.concat(behandling.getAksjonspunkter().stream().map(Aksjonspunkt::getAksjonspunktDefinisjon), aksjonspunkter.stream())
                .filter(other -> !AksjonspunktDefinisjon.KONTROLLER_STOR_ETTERBETALING_SØKER.equals(other))
                .anyMatch(aksjonspunkt -> !aksjonspunkt.erAutopunkt());

            var etterbetalingssum = etterbetalingskontrollResultatOpt.get().etterbetalingssum();
            var behandlingÅrsakerStreng = behandlingsårsakerString(behandling);
            if (etterbetalingssum.compareTo(BigDecimal.valueOf(60_000)) > 0 ) {
                Metrics.counter(COUNTER_ETTERBETALING_NAME, lagTagsForCounter(behandling, harAndreAksjonspunkt, "over_60")).increment();
                LOG.info("Stor etterbetaling til søker over 60_000 med årsaker {}", behandlingÅrsakerStreng);
            } else if (etterbetalingssum.compareTo(BigDecimal.valueOf(30_000)) > 0 ) {
                Metrics.counter(COUNTER_ETTERBETALING_NAME, lagTagsForCounter(behandling, harAndreAksjonspunkt, "mellom_60_og_30")).increment();
                LOG.info("Stor etterbetaling til søker over 30_000 med årsaker {}", behandlingÅrsakerStreng);
            } else if (etterbetalingssum.compareTo(BigDecimal.valueOf(10_000)) > 0 ) {
                Metrics.counter(COUNTER_ETTERBETALING_NAME, lagTagsForCounter(behandling, harAndreAksjonspunkt, "mellom_30_og_10")).increment();
                LOG.info("Stor etterbetaling til søker over 10_000 med årsaker {}", behandlingÅrsakerStreng);
            } else if (etterbetalingssum.compareTo(BigDecimal.ZERO) > 0 ) {
                Metrics.counter(COUNTER_ETTERBETALING_NAME, lagTagsForCounter(behandling, harAndreAksjonspunkt, "over_0_under_10")).increment();
            }
        } catch (Exception e) {
            LOG.info("Noe gikk galt med logging av stor etterbetaling", e);
        }
    }

    private static List<Tag> lagTagsForCounter(Behandling behandling, boolean harAndreAksjonspunkt, String etterbetalingTag) {
        var tags = new ArrayList<Tag>();
        var behandlingsårsaker = behandlingsårsakerString(behandling);
        tags.add(new ImmutableTag("behandlingsaarsaker", behandlingsårsaker));
        tags.add(new ImmutableTag("ytelse", behandling.getFagsakYtelseType().getKode()));
        tags.add(new ImmutableTag("harAndreAksjonspunkt", String.valueOf(harAndreAksjonspunkt)));
        tags.add(new ImmutableTag("etterbetaling_verdi", etterbetalingTag));
        return tags;
    }

    private static String behandlingsårsakerString(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream()
            .map(årsak -> årsak.getBehandlingÅrsakType().getKode())
            .sorted()
            .collect(Collectors.joining(","));
    }

    private Optional<EtterbetalingskontrollResultat> harStorEtterbetalingTilSøker(Behandling behandling) {
        if (behandling.getFagsakYtelseType().equals(FagsakYtelseType.ENGANGSTØNAD)) {
            return Optional.empty(); // Ikke relevant for ES
        }
        var utbetResultat = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId());
        // Ingen utbetaling i denne behandlingen, vil ikke bli etterbetaling
        return utbetResultat.flatMap(beregningsresultatEntitet -> behandling.getOriginalBehandlingId()
            .flatMap(orgId -> beregningsresultatRepository.hentUtbetBeregningsresultat(orgId)
                .map(forrigeRes -> Etterbetalingtjeneste.finnSumSomVilBliEtterbetalt(LocalDate.now(), forrigeRes, beregningsresultatEntitet))));

    }

    private void lagreBrukInntrekk(Behandling behandling, SimuleringResultatDto resultatDto) {
        tilbakekrevingRepository.lagre(behandling, resultatDto.slåttAvInntrekk());
    }

    private LocalDateTime utledNesteKjøring() {
        var currentTime = LocalDateTime.now();
        if (DayOfWeek.SATURDAY.equals(currentTime.getDayOfWeek()) || DayOfWeek.SUNDAY.equals(currentTime.getDayOfWeek())) {
            return kommendeMandag(currentTime);
        }
        if (DayOfWeek.FRIDAY.equals(currentTime.getDayOfWeek()) && currentTime.getHour() > STENGETID) {
            return kommendeMandag(currentTime);
        }
        if (currentTime.getHour() < ÅPNINGSTID) {
            return currentTime.withHour(ÅPNINGSTID).withMinute(15);
        }
        if (currentTime.getHour() > STENGETID) {
            return currentTime.plusDays(1).withHour(ÅPNINGSTID).withMinute(15);
        }
        return null; // bruker default innenfor åpningstid
    }

    private LocalDateTime kommendeMandag(LocalDateTime currentTime) {
        return currentTime.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).withHour(ÅPNINGSTID).withMinute(15);
    }

    private boolean kanOppdatereEksisterendeTilbakekrevingsbehandling(Behandling behandling, SimuleringResultatDto simuleringResultatDto) {
        var harÅpenTilbakekreving = harÅpenTilbakekreving(behandling);
        if (!harÅpenTilbakekreving && SimuleringIntegrasjonTjeneste.harFeilutbetaling(simuleringResultatDto)) {
            LOG.info("Saksnummer {} har ikke åpen tilbakekreving og det er identifisert feilutbetaling. Simuleringsresultat: sumFeilutbetaling={}, sumInntrekk={}, slåttAvInntrekk={}",
                behandling.getSaksnummer(), simuleringResultatDto.sumFeilutbetaling(), simuleringResultatDto.sumInntrekk(), simuleringResultatDto.slåttAvInntrekk());
        }
        return harÅpenTilbakekreving && simuleringResultatDto.sumFeilutbetaling() != 0;
    }

    private boolean harÅpenTilbakekreving(Behandling behandling) {
        return fptilbakeRestKlient.harÅpenTilbakekrevingsbehandling(behandling.getSaksnummer());
    }

    private void lagreTilbakekrevingValg(Behandling behandling, TilbakekrevingValg tilbakekrevingValg) {
        tilbakekrevingRepository.lagre(behandling, tilbakekrevingValg);
    }
}
