package no.nav.foreldrepenger.økonomistøtte.simulering.tjeneste;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.økonomistøtte.SimulerOppdragTjeneste;

@ApplicationScoped
public class SimulerInntrekkSjekkeTjeneste {

    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;
    private SimulerOppdragTjeneste simulerOppdragTjeneste;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private Historikkinnslag2Repository historikkinnslagRepository;

    SimulerInntrekkSjekkeTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SimulerInntrekkSjekkeTjeneste(SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste,
                                         SimulerOppdragTjeneste simulerOppdragTjeneste,
                                         TilbakekrevingRepository tilbakekrevingRepository,
                                         Historikkinnslag2Repository historikkinnslagRepository) {
        this.simuleringIntegrasjonTjeneste = simuleringIntegrasjonTjeneste;
        this.simulerOppdragTjeneste = simulerOppdragTjeneste;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    /**
     * denne tjenesten brukes når vedtak fattes. Hvis det i simulering-steget ikke ble identifiseret feilutbetaling,
     * men det identifiseres på dette tidspunktet, blir tidligere valg angående tilbakekreving reversert da de nå er ugyldige,
     * og automatisk satt til at det skal opprettes tilbakekrevingsbehandling
     */
    public void sjekkIntrekk(Behandling behandling) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            return;
        }
        var tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        if (tilbakekrevingValg.filter(valg -> valg.getVidereBehandling().equals(TilbakekrevingVidereBehandling.INNTREKK)).isPresent()) {
            var oppdragskontroll = simulerOppdragTjeneste.hentOppdragskontrollForBehandling(behandling.getId());
            simuleringIntegrasjonTjeneste.startSimulering(oppdragskontroll);

            var simuleringResultatDto = simuleringIntegrasjonTjeneste.hentResultat(behandling.getId());
            if (simuleringResultatDto.filter(SimuleringIntegrasjonTjeneste::harFeilutbetaling).isPresent()) {
                tilbakekrevingRepository.lagre(behandling,
                    TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, null));
                opprettHistorikkInnslag(behandling.getId(), behandling.getFagsakId());
            }
        }
    }

    private void opprettHistorikkInnslag(Long behandlingId, Long fagsakId) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medBehandlingId(behandlingId)
            .medFagsakId(fagsakId)
            .medTittel(SkjermlenkeType.FAKTA_OM_SIMULERING)
            .addLinje(
                fraTilEquals("Fastsett videre behandling", "Feilutbetalingen er trukket inn i annen utbetaling", "Feilutbetaling med tilbakekreving"))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

}
