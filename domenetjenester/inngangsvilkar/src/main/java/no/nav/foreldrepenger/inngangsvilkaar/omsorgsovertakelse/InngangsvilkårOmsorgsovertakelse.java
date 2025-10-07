package no.nav.foreldrepenger.inngangsvilkaar.omsorgsovertakelse;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;

@ApplicationScoped
@VilkårTypeRef(VilkårType.OMSORGSOVERTAKELSEVILKÅR)
public class InngangsvilkårOmsorgsovertakelse implements Inngangsvilkår {

    public InngangsvilkårOmsorgsovertakelse() {
        // for CDI proxy
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        //alltid manuell
        return new VilkårData(VilkårType.OMSORGSOVERTAKELSEVILKÅR, VilkårUtfallType.IKKE_VURDERT,
            List.of(AksjonspunktDefinisjon.VURDER_OMSORGSOVERTAKELSEVILKÅRET));
    }

}

