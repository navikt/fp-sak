package no.nav.foreldrepenger.inngangsvilkaar.medlemskap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.Medlemskapsvilkår;

@ApplicationScoped
@VilkårTypeRef(VilkårType.MEDLEMSKAPSVILKÅRET)
public class InngangsvilkårMedlemskap implements Inngangsvilkår {

    private InngangsvilkårOversetter inngangsvilkårOversetter;

    InngangsvilkårMedlemskap() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårMedlemskap(InngangsvilkårOversetter inngangsvilkårOversetter) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        var grunnlag = inngangsvilkårOversetter.oversettTilRegelModellMedlemskap(ref);

        var evaluation = new Medlemskapsvilkår().evaluer(grunnlag);

        return InngangsvilkårOversetter.tilVilkårData(VilkårType.MEDLEMSKAPSVILKÅRET, evaluation, grunnlag);
    }
}
