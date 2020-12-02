package no.nav.foreldrepenger.behandling.steg.simulering;

import static java.util.Collections.singletonList;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import no.nav.foreldrepenger.økonomi.simulering.klient.FpoppdragSystembrukerRestKlient;
import no.nav.foreldrepenger.økonomi.simulering.kontrakt.SimuleringResultatDto;
import no.nav.foreldrepenger.økonomi.simulering.tjeneste.SimuleringIntegrasjonTjeneste;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;
import no.nav.foreldrepenger.økonomi.økonomistøtte.SimulerOppdragTjeneste;
import no.nav.vedtak.exception.TekniskException;

@BehandlingStegRef(kode = "SIMOPP")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class SimulerOppdragSteg implements BehandlingSteg {

    public static final long DUMMY_TASK_ID = -1L; // TODO (Team Tonic) simulerOppdrag-tjenesten krever en task-id som input, uten
                                                  // at den skal være i bruk

    private static final int ÅPNINGSTID = 7;
    private static final int STENGETID = 21;

    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private SimulerOppdragTjeneste simulerOppdragTjeneste;
    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private FpoppdragSystembrukerRestKlient fpoppdragSystembrukerRestKlient;
    private FptilbakeRestKlient fptilbakeRestKlient;

    SimulerOppdragSteg() {
        // for CDI proxy
    }

    @Inject
    public SimulerOppdragSteg(BehandlingRepositoryProvider repositoryProvider,
            BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
            SimulerOppdragTjeneste simulerOppdragTjeneste,
            SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste,
            TilbakekrevingRepository tilbakekrevingRepository,
            FpoppdragSystembrukerRestKlient fpoppdragSystembrukerRestKlient,
            FptilbakeRestKlient fptilbakeRestKlient) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.simulerOppdragTjeneste = simulerOppdragTjeneste;
        this.simuleringIntegrasjonTjeneste = simuleringIntegrasjonTjeneste;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.fpoppdragSystembrukerRestKlient = fpoppdragSystembrukerRestKlient;
        this.fptilbakeRestKlient = fptilbakeRestKlient;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        try {
            startSimulering(behandling);
            return utledAksjonspunkt(behandling);
        } catch (TekniskException e) {
            opprettFortsettBehandlingTask(behandling);
            return BehandleStegResultat.settPåVent();
        }
    }

    @Override
    public BehandleStegResultat gjenopptaSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        startSimulering(behandling);
        return utledAksjonspunkt(behandling);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        if (!BehandlingStegType.SIMULER_OPPDRAG.equals(tilSteg)) {
            Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            fpoppdragSystembrukerRestKlient.kansellerSimulering(kontekst.getBehandlingId());
            tilbakekrevingRepository.deaktiverEksisterendeTilbakekrevingValg(behandling);
            tilbakekrevingRepository.deaktiverEksisterendeTilbakekrevingInntrekk(behandling);
        }
    }

    private void startSimulering(Behandling behandling) {
        List<String> oppdragXmler = simulerOppdragTjeneste.simulerOppdrag(behandling.getId(), DUMMY_TASK_ID);
        simuleringIntegrasjonTjeneste.startSimulering(behandling.getId(), oppdragXmler);
    }

    private void opprettFortsettBehandlingTask(Behandling behandling) {
        LocalDateTime nesteKjøringEtter = utledNesteKjøring();
        behandlingProsesseringTjeneste.opprettTasksForFortsettBehandlingGjenopptaStegNesteKjøring(behandling,
                BehandlingStegType.SIMULER_OPPDRAG, nesteKjøringEtter);
    }

    private BehandleStegResultat utledAksjonspunkt(Behandling behandling) {
        Optional<SimuleringResultatDto> simuleringResultatDto = simuleringIntegrasjonTjeneste.hentResultat(behandling.getId());
        if (simuleringResultatDto.isPresent()) {
            SimuleringResultatDto resultatDto = simuleringResultatDto.get();

            // TODO dette har ingenting med utledning av aksjonspunkt å gjøre, og bør
            // flyttes til alternativ metode
            tilbakekrevingRepository.lagre(behandling, resultatDto.isSlåttAvInntrekk());

            if (kanOppdatereEksisterendeTilbakekrevingsbehandling(behandling, resultatDto)) { // vi sender TILBAKEKREVING_OPPDATER når det finnes et
                                                                                              // simulering resultat
                lagreTilbakekrevingValg(behandling, TilbakekrevingValg.medOppdaterTilbakekrevingsbehandling());
                return BehandleStegResultat.utførtUtenAksjonspunkter();
            }

            if (resultatDto.harFeilutbetaling()) {
                return BehandleStegResultat.utførtMedAksjonspunkter(singletonList(AksjonspunktDefinisjon.VURDER_FEILUTBETALING));
            }
            if (resultatDto.harInntrekkmulighet()) {
                lagreTilbakekrevingValg(behandling, TilbakekrevingValg.medAutomatiskInntrekk());
                return BehandleStegResultat.utførtUtenAksjonspunkter();
            }
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private LocalDateTime utledNesteKjøring() {
        LocalDateTime currentTime = LocalDateTime.now();
        if (DayOfWeek.SATURDAY.equals(currentTime.getDayOfWeek()) || DayOfWeek.SUNDAY.equals(currentTime.getDayOfWeek())) {
            return kommendeMandag(currentTime);
        } else if (DayOfWeek.FRIDAY.equals(currentTime.getDayOfWeek()) && (currentTime.getHour() > STENGETID)) {
            return kommendeMandag(currentTime);
        } else if (currentTime.getHour() < ÅPNINGSTID) {
            return currentTime.withHour(ÅPNINGSTID).withMinute(15);
        } else if (currentTime.getHour() > STENGETID) {
            return currentTime.plusDays(1).withHour(ÅPNINGSTID).withMinute(15);
        }
        return null; // bruker default innenfor åpningstid
    }

    private LocalDateTime kommendeMandag(LocalDateTime currentTime) {
        return currentTime.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).withHour(ÅPNINGSTID).withMinute(15);
    }

    private boolean kanOppdatereEksisterendeTilbakekrevingsbehandling(Behandling behandling, SimuleringResultatDto simuleringResultatDto) {
        return harÅpenTilbakekreving(behandling) && (simuleringResultatDto.getSumFeilutbetaling() != 0);
    }

    private boolean harÅpenTilbakekreving(Behandling behandling) {
        return fptilbakeRestKlient.harÅpenTilbakekrevingsbehandling(behandling.getFagsak().getSaksnummer());
    }

    private void lagreTilbakekrevingValg(Behandling behandling, TilbakekrevingValg tilbakekrevingValg) {
        tilbakekrevingRepository.lagre(behandling, tilbakekrevingValg);
    }
}
