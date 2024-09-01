package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt.TilbakekrevingvalgHistorikkinnslagBygger;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt.VurderFeilutbetalingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt.VurderFeilutbetalingOppdaterer;

class VurderFeilutbetalingOppdatererTest {

    private final TilbakekrevingRepository repository = mock(TilbakekrevingRepository.class);
    private final HistorikkTjenesteAdapter historikkTjenesteAdapter = mock(HistorikkTjenesteAdapter.class);
    private final TilbakekrevingvalgHistorikkinnslagBygger historikkInnslagBygger = new TilbakekrevingvalgHistorikkinnslagBygger(historikkTjenesteAdapter);
    private final BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
    private final VurderFeilutbetalingOppdaterer oppdaterer = new VurderFeilutbetalingOppdaterer(repository, historikkInnslagBygger, behandlingRepository);

    private final ArgumentCaptor<TilbakekrevingValg> captor = ArgumentCaptor.forClass(TilbakekrevingValg.class);

    private Behandling behandling;

    @BeforeEach
    public void setup() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        this.behandling = scenario.lagMocked();
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
    }

    @Test
    void skal_lagre_at_videre_behandling_behandle_i_Infotrygd_når_det_er_valgt() {
        var varseltekst = "varsel";
        var dto = new VurderFeilutbetalingDto("lorem ipsum", TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, varseltekst);

        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));

        verify(repository).lagre(eq(behandling), captor.capture());

        var tilbakekrevingValg = captor.getValue();
        assertThat(tilbakekrevingValg.getErTilbakekrevingVilkårOppfylt()).isNull();
        assertThat(tilbakekrevingValg.getGrunnerTilReduksjon()).isNull();
        assertThat(tilbakekrevingValg.getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING);
        assertThat(tilbakekrevingValg.getVarseltekst()).isEqualTo(varseltekst);
    }

    @Test
    void skal_feile_når_Inntrekk_er_forsøkt_valgt() {
        var dto = new VurderFeilutbetalingDto("lorem ipsum", TilbakekrevingVidereBehandling.INNTREKK, null);

        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);
        assertThrows(IllegalArgumentException.class, () -> oppdaterer.oppdater(dto, param));

    }

}
