package no.nav.foreldrepenger.inngangsvilkaar.svp;

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
@VilkårTypeRef(VilkårType.SVANGERSKAPSPENGERVILKÅR)
public class InngangsvilkårSvangerskapspenger implements Inngangsvilkår {

    public InngangsvilkårSvangerskapspenger() {
        // for CDI proxy
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        //alltid manuell
        return new VilkårData(VilkårType.SVANGERSKAPSPENGERVILKÅR, VilkårUtfallType.IKKE_VURDERT,
            List.of(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET));
    }

}

