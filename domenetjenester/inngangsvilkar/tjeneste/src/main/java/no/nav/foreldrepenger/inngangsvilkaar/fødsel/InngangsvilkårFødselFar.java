package no.nav.foreldrepenger.inngangsvilkaar.fødsel;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel.FødselsvilkårFar;

/**
 * Adapter for å evaluere fødselsvilkåret for far.
 */
@ApplicationScoped
@VilkårTypeRef(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR)
class InngangsvilkårFødselFar implements Inngangsvilkår {
    private InngangsvilkårOversetter inngangsvilkårOversetter;

    InngangsvilkårFødselFar() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårFødselFar(InngangsvilkårOversetter inngangsvilkårOversetter) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        var grunnlag = inngangsvilkårOversetter.oversettTilRegelModellFødsel(ref);

        var evaluation = new FødselsvilkårFar().evaluer(grunnlag);

        return InngangsvilkårOversetter.tilVilkårData(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR, evaluation, grunnlag);
    }
}
