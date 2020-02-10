package no.nav.foreldrepenger.inngangsvilkaar.søknad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.søknad.InngangsvilkårSøkersOpplysningsplikt;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;

public class InngangsvilkårSøkersOpplysningspliktTest {

    InngangsvilkårSøkersOpplysningsplikt testObjekt;
    private KompletthetsjekkerProvider kompletthetssjekkerProvider = mock(KompletthetsjekkerProvider.class);
    private Kompletthetsjekker kompletthetssjekker = mock(Kompletthetsjekker.class);

    @Before
    public void setup() {
        kompletthetssjekkerProvider = mock(KompletthetsjekkerProvider.class);
        testObjekt = new InngangsvilkårSøkersOpplysningsplikt(kompletthetssjekkerProvider);
    }

    @Test
    public void komplett_søknad_skal_medføre_oppfylt() {
        when(kompletthetssjekkerProvider.finnKompletthetsjekkerFor(any(), any())).thenReturn(kompletthetssjekker);
        when(kompletthetssjekker.erForsendelsesgrunnlagKomplett(any()))
            .thenReturn(true);
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();

        VilkårData vilkårData = testObjekt.vurderVilkår(lagRef(behandling));

        assertThat(vilkårData).isNotNull();
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.SØKERSOPPLYSNINGSPLIKT);
        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(vilkårData.getApDefinisjoner()).isEmpty();
    }

    @Test
    public void ikke_komplett_søknad_skal_medføre_manuell_vurdering() {
        when(kompletthetssjekkerProvider.finnKompletthetsjekkerFor(any(), any())).thenReturn(kompletthetssjekker);
        when(kompletthetssjekker.erForsendelsesgrunnlagKomplett(any()))
            .thenReturn(false);
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();

        VilkårData vilkårData = testObjekt.vurderVilkår(lagRef(behandling));

        assertThat(vilkårData).isNotNull();
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.SØKERSOPPLYSNINGSPLIKT);
        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_VURDERT);
        assertThat(vilkårData.getApDefinisjoner()).hasSize(1);
        assertThat(vilkårData.getApDefinisjoner()).contains(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU);
    }

    @Test
    public void revurdering_for_foreldrepenger_skal_alltid_medføre_oppfylt() {
        when(kompletthetssjekkerProvider.finnKompletthetsjekkerFor(any(), any())).thenReturn(kompletthetssjekker);
        when(kompletthetssjekker.erForsendelsesgrunnlagKomplett(any()))
            .thenReturn(false);
        Behandling revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();

        VilkårData vilkårData = testObjekt.vurderVilkår(lagRef(revurdering));

        assertThat(vilkårData).isNotNull();
        assertThat(vilkårData.getVilkårType()).isEqualTo(VilkårType.SØKERSOPPLYSNINGSPLIKT);
        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(vilkårData.getApDefinisjoner()).isEmpty();
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

}
