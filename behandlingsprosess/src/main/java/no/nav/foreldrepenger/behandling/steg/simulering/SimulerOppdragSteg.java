package no.nav.foreldrepenger.behandling.steg.simulering;

import static java.util.Collections.singletonList;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;
import no.nav.foreldrepenger.økonomistøtte.SimulerOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.simulering.klient.FpoppdragSystembrukerRestKlient;
import no.nav.foreldrepenger.økonomistøtte.simulering.kontrakt.SimuleringResultatDto;
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
            fpoppdragSystembrukerRestKlient.kansellerSimulering(kontekst.getBehandlingId());
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
        var simuleringResultatDto = simuleringIntegrasjonTjeneste.hentResultat(behandling.getId());
        if (simuleringResultatDto.isPresent()) {
            var resultatDto = simuleringResultatDto.get();

            lagreBrukInntrekk(behandling, resultatDto);

            // vi sender TILBAKEKREVING_OPPDATER når det finnes et simulering resultat
            if (kanOppdatereEksisterendeTilbakekrevingsbehandling(behandling, resultatDto)) {
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

    private void lagreBrukInntrekk(Behandling behandling, SimuleringResultatDto resultatDto) {
        tilbakekrevingRepository.lagre(behandling, resultatDto.isSlåttAvInntrekk());
    }

    private LocalDateTime utledNesteKjøring() {
        var currentTime = LocalDateTime.now();
        if (DayOfWeek.SATURDAY.equals(currentTime.getDayOfWeek()) || DayOfWeek.SUNDAY.equals(currentTime.getDayOfWeek())) {
            return kommendeMandag(currentTime);
        }
        if (DayOfWeek.FRIDAY.equals(currentTime.getDayOfWeek()) && (currentTime.getHour() > STENGETID)) {
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
        if (!harÅpenTilbakekreving && simuleringResultatDto.harFeilutbetaling()) {
            LOG.info("Saksnummer {} har ikke åpen tilbakekreving og det er identifisert feilutbetaling. Simuleringsresultat: sumFeilutbetaling={}, sumInntrekk={}, slåttAvInntrekk={}",
                behandling.getFagsak().getSaksnummer(), simuleringResultatDto.getSumFeilutbetaling(), simuleringResultatDto.getSumInntrekk(), simuleringResultatDto.isSlåttAvInntrekk());
        }
        return harÅpenTilbakekreving && (simuleringResultatDto.getSumFeilutbetaling() != 0);
    }

    private boolean harÅpenTilbakekreving(Behandling behandling) {
        return fptilbakeRestKlient.harÅpenTilbakekrevingsbehandling(behandling.getFagsak().getSaksnummer());
    }

    private void lagreTilbakekrevingValg(Behandling behandling, TilbakekrevingValg tilbakekrevingValg) {
        tilbakekrevingRepository.lagre(behandling, tilbakekrevingValg);
    }
}
