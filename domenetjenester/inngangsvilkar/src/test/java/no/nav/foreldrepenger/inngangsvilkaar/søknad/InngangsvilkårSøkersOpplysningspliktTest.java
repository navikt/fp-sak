package no.nav.foreldrepenger.inngangsvilkaar.søknad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

class InngangsvilkårSøkersOpplysningspliktTest {

    @Test
    void komplett_søknad_skal_medføre_oppfylt() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();

        var vilkårData = new InngangsvilkårSøkersOpplysningsplikt().vurderVilkår(BehandlingReferanse.fra(behandling));

        assertThat(vilkårData).isNotNull();
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.SØKERSOPPLYSNINGSPLIKT);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(vilkårData.aksjonspunktDefinisjoner()).isEmpty();
    }

}
