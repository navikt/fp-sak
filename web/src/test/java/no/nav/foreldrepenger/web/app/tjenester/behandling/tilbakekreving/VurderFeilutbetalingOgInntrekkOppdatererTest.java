package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt.TilbakekrevingvalgHistorikkinnslagBygger;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt.VurderFeilutbetalingOgInntrekkDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt.VurderFeilutbetalingOgInntrekkOppdaterer;

public class VurderFeilutbetalingOgInntrekkOppdatererTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private TilbakekrevingRepository repository = Mockito.mock(TilbakekrevingRepository.class);
    private HistorikkTjenesteAdapter historikkTjenesteAdapter = Mockito.mock(HistorikkTjenesteAdapter.class);
    private TilbakekrevingvalgHistorikkinnslagBygger historikkInnslagBygger = new TilbakekrevingvalgHistorikkinnslagBygger(historikkTjenesteAdapter);
    private VurderFeilutbetalingOgInntrekkOppdaterer oppdaterer = new VurderFeilutbetalingOgInntrekkOppdaterer(repository, historikkInnslagBygger);

    private ArgumentCaptor<TilbakekrevingValg> captor = ArgumentCaptor.forClass(TilbakekrevingValg.class);

    private Behandling behandling;

    @Before
    public void setup() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        this.behandling = scenario.lagMocked();
    }

    @Test
    public void skal_lagre_at_videre_behandling_er_med_inntrekk_når_riktige_felter_er_valgt() {
        var dto = new VurderFeilutbetalingOgInntrekkDto("lorem ipsum", true, false, null);
        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        Mockito.verify(repository).lagre(Mockito.eq(behandling), captor.capture());

        TilbakekrevingValg tilbakekrevingValg = captor.getValue();
        assertThat(tilbakekrevingValg.getErTilbakekrevingVilkårOppfylt()).isTrue();
        assertThat(tilbakekrevingValg.getGrunnerTilReduksjon()).isFalse();
        assertThat(tilbakekrevingValg.getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.INNTREKK);
    }

    @Test
    public void skal_lagre_at_videre_behandling_er_behandle_i_infotrygd_når_det_er_valgt() {
        var dto = new VurderFeilutbetalingOgInntrekkDto("lorem ipsum", true, true, TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD);
        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        Mockito.verify(repository).lagre(Mockito.eq(behandling), captor.capture());

        TilbakekrevingValg tilbakekrevingValg = captor.getValue();
        assertThat(tilbakekrevingValg.getErTilbakekrevingVilkårOppfylt()).isTrue();
        assertThat(tilbakekrevingValg.getGrunnerTilReduksjon()).isTrue();
        assertThat(tilbakekrevingValg.getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD);
    }

    @Test
    public void skal_feile_når_boolske_variable_indikerer_inntrekk_men_noe_annet_er_valgt() {
        var dto = new VurderFeilutbetalingOgInntrekkDto("lorem ipsum", true, false, TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD);

        expectedException.expect(IllegalArgumentException.class);
        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

    }

}
