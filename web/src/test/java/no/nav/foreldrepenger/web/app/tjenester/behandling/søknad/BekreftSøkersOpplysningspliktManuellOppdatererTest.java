package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt.BekreftSokersOpplysningspliktManuDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt.BekreftSøkersOpplysningspliktManuellOppdaterer;

class BekreftSøkersOpplysningspliktManuellOppdatererTest {

    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_søkers_opplysningsplikt_manu() {
        // Arrange
        // Behandling
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU,
                BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT);
        scenario.lagMocked();

        var behandling = scenario.getBehandling();

        var oppdaterer = new BekreftSøkersOpplysningspliktManuellOppdaterer(lagMockHistory(), scenario.mockBehandlingRepository());

        // Dto
        var bekreftSokersOpplysningspliktManuDto = new BekreftSokersOpplysningspliktManuDto(
                "test av manu", true, Collections.emptyList());
        assertThat(behandling.getAksjonspunkter()).hasSize(1);

        // Act
        var aksjonspunkt = behandling.getAksjonspunktFor(bekreftSokersOpplysningspliktManuDto.getAksjonspunktDefinisjon());
        var resultat = oppdaterer.oppdater(bekreftSokersOpplysningspliktManuDto,
                new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), bekreftSokersOpplysningspliktManuDto, aksjonspunkt));
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        var historikkInnslag = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(historikkInnslag).hasSize(1);
        var del = historikkInnslag.get(0);
        var feltList = del.getEndredeFelt();
        assertThat(feltList).hasSize(1);
        var felt = feltList.get(0);
        assertThat(felt.getNavn()).as("navn").isEqualTo(HistorikkEndretFeltType.SOKERSOPPLYSNINGSPLIKT.getKode());
        assertThat(felt.getFraVerdi()).as("fraVerdi").isNull();
        assertThat(felt.getTilVerdi()).as("tilVerdi").isEqualTo(HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT.getKode());

        var aksjonspunktSet = resultat.getEkstraAksjonspunktResultat().stream()
                .map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(Collectors.toSet());

        assertThat(aksjonspunktSet).isEmpty();
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        var mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }
}
