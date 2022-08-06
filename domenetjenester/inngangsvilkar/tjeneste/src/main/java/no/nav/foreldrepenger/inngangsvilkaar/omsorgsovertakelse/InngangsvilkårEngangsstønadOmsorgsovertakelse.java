package no.nav.foreldrepenger.inngangsvilkaar.omsorgsovertakelse;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;

@ApplicationScoped
@VilkårTypeRef(VilkårType.OMSORGSVILKÅRET)
public class InngangsvilkårEngangsstønadOmsorgsovertakelse implements Inngangsvilkår {

    public InngangsvilkårEngangsstønadOmsorgsovertakelse() {
        // for CDI proxy
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        return new VilkårData(VilkårType.OMSORGSVILKÅRET, VilkårUtfallType.IKKE_VURDERT,
                List.of(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET));
    }

}
