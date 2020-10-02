package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;

public class AksjonspunktUtlederForTilleggsopplysningerTest {

    @Test
    public void skal_returnere_aksjonspunkt_for_tilleggsopplysninger_dersom_det_er_oppgitt_i_søknad() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenarioMedTillegsopplysningerPåSøknad = ScenarioMorSøkerEngangsstønad
                .forFødsel()
                .medTilleggsopplysninger("Tillegsopplysninger");
        // Trenger bare behandling. Andre fakta settes til null
        Behandling behandling = scenarioMedTillegsopplysningerPåSøknad.lagMocked();

        // Act
        AksjonspunktUtlederForTilleggsopplysninger aksjonspunktUtleder = new AksjonspunktUtlederForTilleggsopplysninger(
                scenarioMedTillegsopplysningerPåSøknad.mockBehandlingRepositoryProvider());
        List<AksjonspunktResultat> apResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(apResultater).hasSize(1);
        assertThat(apResultater.get(0).getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AVKLAR_TILLEGGSOPPLYSNINGER);
    }

    @Test
    public void skal_returnere_ingen_aksjonspunkt_for_tilleggsopplysninger_dersom_det_ikke_er_oppgitt_i_søknad() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenarioUtenTillegsopplysningerPåSøknad = ScenarioMorSøkerEngangsstønad
                .forFødsel();
        // Trenger bare behandling. Andre fakta settes til null
        Behandling behandling = scenarioUtenTillegsopplysningerPåSøknad.lagMocked();

        // Act
        AksjonspunktUtlederForTilleggsopplysninger aksjonspunktUtleder = new AksjonspunktUtlederForTilleggsopplysninger(
                scenarioUtenTillegsopplysningerPåSøknad.mockBehandlingRepositoryProvider());
        List<AksjonspunktResultat> apResultater = aksjonspunktUtleder.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(apResultater).isEmpty();
    }

    private AksjonspunktUtlederInput lagInput(Behandling behandling) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling));
    }
}
