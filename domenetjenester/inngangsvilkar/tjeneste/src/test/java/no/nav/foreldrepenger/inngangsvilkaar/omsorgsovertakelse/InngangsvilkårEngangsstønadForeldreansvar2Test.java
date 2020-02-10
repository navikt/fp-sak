package no.nav.foreldrepenger.inngangsvilkaar.omsorgsovertakelse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.omsorgsovertakelse.InngangsvilkårEngangsstønadForeldreansvar2;

public class InngangsvilkårEngangsstønadForeldreansvar2Test {

    @Test
    public void skal_uavhengig_av_behandling_alltid_opprette_aksjonspunkt_for_manuell_vurdering() {
        VilkårData vilkårData = new InngangsvilkårEngangsstønadForeldreansvar2().vurderVilkår(null);

        assertThat(vilkårData.getUtfallType()).isEqualTo(VilkårUtfallType.IKKE_VURDERT);
        assertThat(vilkårData.getApDefinisjoner()).hasSize(1);
        assertThat(vilkårData.getApDefinisjoner().get(0)).isEqualTo(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_4_LEDD);
    }

}
