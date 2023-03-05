package no.nav.foreldrepenger.økonomistøtte.simulering.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.SimulerOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.simulering.kontrakt.SimuleringResultatDto;

class SimulerInntrekkSjekkeTjenesteTest {

    private SimulerInntrekkSjekkeTjeneste simulerInntrekkSjekkeTjeneste;
    private Behandling behandling;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private HistorikkRepository historikkRepository;
    private SimulerOppdragTjeneste simulerOppdragTjeneste;
    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;
    private ArgumentCaptor<Historikkinnslag> hisrotikkInnslagCaptor = ArgumentCaptor.forClass(Historikkinnslag.class);

    @BeforeEach
    public void setUp() {
        simuleringIntegrasjonTjeneste = mock(SimuleringIntegrasjonTjeneste.class);
        simulerOppdragTjeneste = mock(SimulerOppdragTjeneste.class);
        tilbakekrevingRepository = mock(TilbakekrevingRepository.class);
        historikkRepository = mock(HistorikkRepository.class);
        behandling = Behandling.nyBehandlingFor(
            Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()), new Saksnummer("123456789")),
            BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandling.setId(123L);
        simulerInntrekkSjekkeTjeneste = new SimulerInntrekkSjekkeTjeneste(simuleringIntegrasjonTjeneste, simulerOppdragTjeneste,
            tilbakekrevingRepository, historikkRepository);
    }

    @Test
    void sjekkIntrekk_når_Tilbakekreving_valg_ikke_finnes() {
        when(tilbakekrevingRepository.hent(anyLong())).thenReturn(Optional.empty());
        simulerInntrekkSjekkeTjeneste.sjekkIntrekk(behandling);
        verify(tilbakekrevingRepository, times(1)).hent(anyLong());
        verify(simulerOppdragTjeneste, never()).hentOppdragskontrollForBehandling(anyLong());
    }

    @Test
    void sjekkIntrekk_når_Tilbakekreving_valg_ikke_inntrekk() {
        when(tilbakekrevingRepository.hent(anyLong())).thenReturn(opprettTilbakekrevingValg(TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD));
        simulerInntrekkSjekkeTjeneste.sjekkIntrekk(behandling);
        verify(tilbakekrevingRepository, times(1)).hent(anyLong());
        verify(simulerOppdragTjeneste, never()).hentOppdragskontrollForBehandling(anyLong());
    }

    @Test
    void sjekkIntrekk_når_aksjonpunkt_er_vurder_intrekk() {
        when(tilbakekrevingRepository.hent(anyLong())).thenReturn(opprettTilbakekrevingValg(TilbakekrevingVidereBehandling.INNTREKK));
        when(simuleringIntegrasjonTjeneste.hentResultat(anyLong())).thenReturn(Optional.of(new SimuleringResultatDto(0L, -2345L, false)));
        simulerInntrekkSjekkeTjeneste.sjekkIntrekk(behandling);
        verify(tilbakekrevingRepository, times(1)).hent(anyLong());
        verify(simulerOppdragTjeneste, times(1)).hentOppdragskontrollForBehandling(anyLong());
        verify(tilbakekrevingRepository, never()).lagre(any(Behandling.class), any(TilbakekrevingValg.class));
    }

    @Test
    void sjekkIntrekk_når_aksjonpunkt_er_vurder_feilutbetaling() {
        when(tilbakekrevingRepository.hent(anyLong())).thenReturn(opprettTilbakekrevingValg(TilbakekrevingVidereBehandling.INNTREKK));
        when(simuleringIntegrasjonTjeneste.hentResultat(anyLong())).thenReturn(Optional.of(new SimuleringResultatDto(-2345L, 0L, false)));
        simulerInntrekkSjekkeTjeneste.sjekkIntrekk(behandling);
        verify(historikkRepository, times(1)).lagre(hisrotikkInnslagCaptor.capture());
        var historikkinnslag = hisrotikkInnslagCaptor.getValue();
        assertThat(historikkinnslag).isNotNull();
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    private Optional<TilbakekrevingValg> opprettTilbakekrevingValg(TilbakekrevingVidereBehandling tilbakekrevingVidereBehandling) {
        return Optional.of(TilbakekrevingValg.utenMulighetForInntrekk(tilbakekrevingVidereBehandling, "test"));
    }
}
