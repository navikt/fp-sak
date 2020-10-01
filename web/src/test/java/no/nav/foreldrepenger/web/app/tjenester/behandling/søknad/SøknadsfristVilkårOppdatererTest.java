package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt.SoknadsfristAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt.SøknadsfristOppdaterer;

public class SøknadsfristVilkårOppdatererTest {
    private HistorikkInnslagTekstBuilder tekstBuilder;

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }

    @Test
    public void skal_generere_historikkinnslag_ved_avklaring_av_soknadsfrist() {
        // Arrange
        boolean oppdatertSoknadsfristOk = true;

        // Behandling
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.ADOPTERER_ALENE);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET,
                BehandlingStegType.VURDER_SØKNADSFRISTVILKÅR);
        scenario.lagMocked();

        Behandling behandling = scenario.getBehandling();
        tekstBuilder = new HistorikkInnslagTekstBuilder();
        SøknadsfristOppdaterer oppdaterer = new SøknadsfristOppdaterer(lagMockHistory());

        // Dto
        SoknadsfristAksjonspunktDto dto = new SoknadsfristAksjonspunktDto("begrunnelse", oppdatertSoknadsfristOk);

        // Act
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        List<HistorikkinnslagDel> historikkInnslag = this.tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(historikkInnslag).hasSize(1);

        HistorikkinnslagDel del = historikkInnslag.get(0);
        List<HistorikkinnslagFelt> feltList = del.getEndredeFelt();
        assertThat(feltList).hasSize(1);
        Optional<HistorikkinnslagFelt> feltOpt = del.getEndretFelt(HistorikkEndretFeltType.SOKNADSFRISTVILKARET);
        assertThat(feltOpt).as("endretFelt[SOKNADSFRISTVILKARET]").hasValueSatisfying(felt -> {
            assertThat(felt.getNavn()).isEqualTo(HistorikkEndretFeltType.SOKNADSFRISTVILKARET.getKode());
            assertThat(felt.getFraVerdi()).isNull();
            assertThat(felt.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.OPPFYLT.getKode());
        });
    }

}
