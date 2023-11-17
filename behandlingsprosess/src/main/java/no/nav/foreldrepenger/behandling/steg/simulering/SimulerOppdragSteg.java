package no.nav.foreldrepenger.behandling.steg.simulering;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.beregnytelse.EtterbetalingskontrollResultat;
import no.nav.foreldrepenger.behandling.steg.beregnytelse.Etterbetalingtjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.kontrakter.simulering.resultat.v1.SimuleringResultatDto;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;
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
            fpOppdragRestKlient.kansellerSimulering(kontekst.getBehandlingId());
            tilbakekrevingRepository.deaktiverEksisterendeTilbakekrevingValg(behandling);
            tilbakekrevingRepository.deaktiverEksisterendeTilbakekrevingInntrekk(behandling);
        }
    }

    private void startSimulering(Behandling behandling) {
        var oppdragskontroll = simulerOppdragTjeneste.hentOppdragskontrollForBehandling(behandling.getId());
        simuleringIntegrasjonTjeneste.startSimulering(oppdragskontroll);
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
            // TODO legg inn denne etter at frontend er klar
            // TODO Sjekk om det er mulig å både få VURDER_FEILUTBETALING og KONTROLLER_STOR_ETTERBETALING_SØKER samtidig
            // aksjonspunkter.add(AksjonspunktDefinisjon.KONTROLLER_STOR_ETTERBETALING_SØKER);
            LOG.info("Høy etterbetaling på kr {} til bruker på sak {}", etterbetalingskontrollResultat.get().etterbetalingssum(),
                behandling.getFagsak().getSaksnummer());
        }

        var simuleringResultatDto = simuleringIntegrasjonTjeneste.hentResultat(behandling.getId());
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
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
    }

    private Optional<EtterbetalingskontrollResultat> harStorEtterbetalingTilSøker(Behandling behandling) {
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
                behandling.getFagsak().getSaksnummer(), simuleringResultatDto.sumFeilutbetaling(), simuleringResultatDto.sumInntrekk(), simuleringResultatDto.slåttAvInntrekk());
        }
        return harÅpenTilbakekreving && simuleringResultatDto.sumFeilutbetaling() != 0;
    }

    private boolean harÅpenTilbakekreving(Behandling behandling) {
        return fptilbakeRestKlient.harÅpenTilbakekrevingsbehandling(behandling.getFagsak().getSaksnummer());
    }

    private void lagreTilbakekrevingValg(Behandling behandling, TilbakekrevingValg tilbakekrevingValg) {
        tilbakekrevingRepository.lagre(behandling, tilbakekrevingValg);
    }
}
