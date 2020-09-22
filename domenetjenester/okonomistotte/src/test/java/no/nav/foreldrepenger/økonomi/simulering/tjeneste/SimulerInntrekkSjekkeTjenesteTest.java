package no.nav.foreldrepenger.økonomi.simulering.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.økonomi.simulering.kontrakt.SimuleringResultatDto;
import no.nav.foreldrepenger.økonomi.økonomistøtte.SimulerOppdragApplikasjonTjeneste;

public class SimulerInntrekkSjekkeTjenesteTest {

    private SimulerInntrekkSjekkeTjeneste simulerInntrekkSjekkeTjeneste;
    private Behandling behandling;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private HistorikkRepository historikkRepository;
    private SimulerOppdragApplikasjonTjeneste simulerOppdragApplikasjonTjeneste;
    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;
    private ArgumentCaptor<Historikkinnslag> hisrotikkInnslagCaptor = ArgumentCaptor.forClass(Historikkinnslag.class);

    @Before
    public void setUp() {
        simuleringIntegrasjonTjeneste = mock(SimuleringIntegrasjonTjeneste.class);
        simulerOppdragApplikasjonTjeneste = mock(SimulerOppdragApplikasjonTjeneste.class);
        tilbakekrevingRepository = mock(TilbakekrevingRepository.class);
        historikkRepository = mock(HistorikkRepository.class);
        behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        simulerInntrekkSjekkeTjeneste = new SimulerInntrekkSjekkeTjeneste(simuleringIntegrasjonTjeneste,
            simulerOppdragApplikasjonTjeneste, tilbakekrevingRepository, historikkRepository);
    }

    @Test
    public void sjekkIntrekk_når_Tilbakekreving_valg_ikke_finnes() {
        when(tilbakekrevingRepository.hent(anyLong())).thenReturn(Optional.empty());
        simulerInntrekkSjekkeTjeneste.sjekkIntrekk(behandling);
        verify(tilbakekrevingRepository, times(1)).hent(anyLong());
        verify(simulerOppdragApplikasjonTjeneste, never()).simulerOppdrag(anyLong(), anyLong());
    }

    @Test
    public void sjekkIntrekk_når_Tilbakekreving_valg_ikke_inntrekk() {
        when(tilbakekrevingRepository.hent(anyLong())).thenReturn(opprettTilbakekrevingValg(TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD));
        simulerInntrekkSjekkeTjeneste.sjekkIntrekk(behandling);
        verify(tilbakekrevingRepository, times(1)).hent(anyLong());
        verify(simulerOppdragApplikasjonTjeneste, never()).simulerOppdrag(anyLong(), anyLong());
    }

    @Test
    public void sjekkIntrekk_når_aksjonpunkt_er_vurder_intrekk() {
        when(tilbakekrevingRepository.hent(anyLong())).thenReturn(opprettTilbakekrevingValg(TilbakekrevingVidereBehandling.INNTREKK));
        when(simuleringIntegrasjonTjeneste.hentResultat(anyLong())).thenReturn(Optional.of(new SimuleringResultatDto(0L, -2345L, false)));
        simulerInntrekkSjekkeTjeneste.sjekkIntrekk(behandling);
        verify(tilbakekrevingRepository, times(1)).hent(anyLong());
        verify(simulerOppdragApplikasjonTjeneste, times(1)).simulerOppdrag(anyLong(), anyLong());
        verify(tilbakekrevingRepository, never()).lagre(any(Behandling.class), any(TilbakekrevingValg.class));
    }

    @Test
    public void sjekkIntrekk_når_aksjonpunkt_er_vurder_feilutbetaling() {
        when(tilbakekrevingRepository.hent(anyLong())).thenReturn(opprettTilbakekrevingValg(TilbakekrevingVidereBehandling.INNTREKK));
        when(simuleringIntegrasjonTjeneste.hentResultat(anyLong())).thenReturn(Optional.of(new SimuleringResultatDto(-2345L, 0L, false)));
        simulerInntrekkSjekkeTjeneste.sjekkIntrekk(behandling);
        verify(historikkRepository, times(1)).lagre(hisrotikkInnslagCaptor.capture());
        Historikkinnslag historikkinnslag = hisrotikkInnslagCaptor.getValue();
        assertThat(historikkinnslag).isNotNull();
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    private Optional<TilbakekrevingValg> opprettTilbakekrevingValg(TilbakekrevingVidereBehandling tilbakekrevingVidereBehandling) {
        return Optional.of(TilbakekrevingValg.utenMulighetForInntrekk(tilbakekrevingVidereBehandling, "test"));
    }
}
