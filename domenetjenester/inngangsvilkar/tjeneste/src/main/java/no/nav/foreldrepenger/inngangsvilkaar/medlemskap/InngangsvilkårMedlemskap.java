package no.nav.foreldrepenger.inngangsvilkaar.medlemskap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultatOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.InngangsvilkårRegler;

@ApplicationScoped
@VilkårTypeRef(VilkårType.MEDLEMSKAPSVILKÅRET)
public class InngangsvilkårMedlemskap implements Inngangsvilkår {

    private MedlemsvilkårOversetter medlemsvilkårOversetter;

    InngangsvilkårMedlemskap() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårMedlemskap(MedlemsvilkårOversetter medlemsvilkårOversetter) {
        this.medlemsvilkårOversetter = medlemsvilkårOversetter;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        var grunnlag = medlemsvilkårOversetter.oversettTilRegelModellMedlemskap(ref);

        var resultat = InngangsvilkårRegler.medlemskap(grunnlag);

        return RegelResultatOversetter.oversett(VilkårType.MEDLEMSKAPSVILKÅRET, resultat);

    }
}
