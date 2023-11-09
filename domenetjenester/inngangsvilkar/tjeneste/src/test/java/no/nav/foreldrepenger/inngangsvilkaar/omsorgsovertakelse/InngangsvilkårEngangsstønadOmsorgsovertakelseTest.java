package no.nav.foreldrepenger.inngangsvilkaar.omsorgsovertakelse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

class InngangsvilkårEngangsstønadOmsorgsovertakelseTest {

    @Test
    void skal_uavhengig_av_behandling_alltid_opprette_aksjonspunkt_for_manuell_vurdering() {
        var vilkårData = new InngangsvilkårEngangsstønadOmsorgsovertakelse().vurderVilkår(null);

        assertThat(vilkårData.utfallType()).isEqualTo(VilkårUtfallType.IKKE_VURDERT);
        assertThat(vilkårData.aksjonspunktDefinisjoner()).hasSize(1);
        assertThat(vilkårData.aksjonspunktDefinisjoner().get(0)).isEqualTo(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET);
    }

}
