package no.nav.foreldrepenger.inngangsvilkaar.medlemskap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.AvklarMedlemskapUtleder;

@ApplicationScoped
@VilkårTypeRef(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE)
public class InngangsvilkårMedlemskapForutgående implements Inngangsvilkår {

    private AvklarMedlemskapUtleder avklarMedlemskapUtleder;

    InngangsvilkårMedlemskapForutgående() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårMedlemskapForutgående(AvklarMedlemskapUtleder avklarMedlemskapUtleder) {
        this.avklarMedlemskapUtleder = avklarMedlemskapUtleder;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        return avklarMedlemskapUtleder.utledFor(ref, VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE);
    }

}
