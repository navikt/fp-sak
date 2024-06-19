package no.nav.foreldrepenger.inngangsvilkaar.søknad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;

class InngangsvilkårSøkersOpplysningspliktTest {

    InngangsvilkårSøkersOpplysningsplikt testObjekt;
    private KompletthetsjekkerProvider kompletthetssjekkerProvider = mock(KompletthetsjekkerProvider.class);
    private Kompletthetsjekker kompletthetssjekker = mock(Kompletthetsjekker.class);

    @BeforeEach
    public void setup() {
        kompletthetssjekkerProvider = mock(KompletthetsjekkerProvider.class);
        testObjekt = new InngangsvilkårSøkersOpplysningsplikt(kompletthetssjekkerProvider);
    }

    @Test
    void komplett_søknad_skal_medføre_oppfylt() {
        when(kompletthetssjekkerProvider.finnKompletthetsjekkerFor(any(), any())).thenReturn(kompletthetssjekker);
        when(kompletthetssjekker.erForsendelsesgrunnlagKomplett(any()))
            .thenReturn(true);
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();

        var vilkårData = testObjekt.vurderVilkår(lagRef(behandling));

        assertThat(vilkårData).isNotNull();
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.SØKERSOPPLYSNINGSPLIKT);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(vilkårData.aksjonspunktDefinisjoner()).isEmpty();
    }

    @Test
    void ikke_komplett_søknad_skal_medføre_manuell_vurdering() {
        when(kompletthetssjekkerProvider.finnKompletthetsjekkerFor(any(), any())).thenReturn(kompletthetssjekker);
        when(kompletthetssjekker.erForsendelsesgrunnlagKomplett(any()))
            .thenReturn(false);
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();

        var vilkårData = testObjekt.vurderVilkår(lagRef(behandling));

        assertThat(vilkårData).isNotNull();
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.SØKERSOPPLYSNINGSPLIKT);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_VURDERT);
        assertThat(vilkårData.aksjonspunktDefinisjoner()).hasSize(1);
        assertThat(vilkårData.aksjonspunktDefinisjoner()).contains(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU);
    }

    @Test
    void revurdering_for_foreldrepenger_skal_alltid_medføre_oppfylt() {
        when(kompletthetssjekkerProvider.finnKompletthetsjekkerFor(any(), any())).thenReturn(kompletthetssjekker);
        when(kompletthetssjekker.erForsendelsesgrunnlagKomplett(any()))
            .thenReturn(false);
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();

        var vilkårData = testObjekt.vurderVilkår(lagRef(revurdering));

        assertThat(vilkårData).isNotNull();
        assertThat(vilkårData.vilkårType()).isEqualTo(VilkårType.SØKERSOPPLYSNINGSPLIKT);
        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(vilkårData.aksjonspunktDefinisjoner()).isEmpty();
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

}
