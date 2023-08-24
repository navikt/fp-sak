package no.nav.foreldrepenger.inngangsvilkaar.omsorgsovertakelse;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;

import java.util.List;

@ApplicationScoped
@VilkårTypeRef(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
public class InngangsvilkårEngangsstønadForeldreansvar1 implements Inngangsvilkår {

    public InngangsvilkårEngangsstønadForeldreansvar1() {
        // for CDI proxy
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        return new VilkårData(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.IKKE_VURDERT,
                List.of(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_2_LEDD));
    }
}
