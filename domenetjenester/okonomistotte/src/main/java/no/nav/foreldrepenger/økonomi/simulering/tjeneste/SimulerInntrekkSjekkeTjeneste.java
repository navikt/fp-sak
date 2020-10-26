package no.nav.foreldrepenger.økonomi.simulering.tjeneste;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.økonomi.simulering.kontrakt.SimuleringResultatDto;
import no.nav.foreldrepenger.økonomi.økonomistøtte.SimulerOppdragApplikasjonTjeneste;

@ApplicationScoped
public class SimulerInntrekkSjekkeTjeneste {

    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;
    private SimulerOppdragApplikasjonTjeneste simulerOppdragTjeneste;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private HistorikkRepository historikkRepository;

    private static final long DUMMY_TASK_ID = -1L;

    SimulerInntrekkSjekkeTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SimulerInntrekkSjekkeTjeneste(SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste,
                                         SimulerOppdragApplikasjonTjeneste simulerOppdragTjeneste,
                                         TilbakekrevingRepository tilbakekrevingRepository,
                                         HistorikkRepository historikkRepository) {
        this.simuleringIntegrasjonTjeneste = simuleringIntegrasjonTjeneste;
        this.simulerOppdragTjeneste = simulerOppdragTjeneste;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.historikkRepository = historikkRepository;
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
        Optional<TilbakekrevingValg> tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        if (tilbakekrevingValg.filter(valg -> valg.getVidereBehandling().equals(TilbakekrevingVidereBehandling.INNTREKK)).isPresent()) {
            List<String> oppdragXmler = simulerOppdragTjeneste.simulerOppdrag(behandling.getId(), DUMMY_TASK_ID);
            simuleringIntegrasjonTjeneste.startSimulering(behandling.getId(), oppdragXmler);

            Optional<SimuleringResultatDto> simuleringResultatDto = simuleringIntegrasjonTjeneste.hentResultat(behandling.getId());
            if (simuleringResultatDto.isPresent() && simuleringResultatDto.get().harFeilutbetaling()) {
                tilbakekrevingRepository.lagre(behandling, TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD, null));
                opprettHistorikkInnslag(behandling.getId());
            }
        }
    }

    private void opprettHistorikkInnslag(Long behandlingId) {
        Historikkinnslag innslag = new Historikkinnslag();
        innslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        innslag.setBehandlingId(behandlingId);
        innslag.setType(HistorikkinnslagType.TILBAKEKREVING_VIDEREBEHANDLING);

        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder().medSkjermlenke(SkjermlenkeType.FAKTA_OM_SIMULERING);
        tekstBuilder.medHendelse(innslag.getType());
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.FASTSETT_VIDERE_BEHANDLING, TilbakekrevingVidereBehandling.INNTREKK, TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD);
        tekstBuilder.build(innslag);
        historikkRepository.lagre(innslag);
    }

}
