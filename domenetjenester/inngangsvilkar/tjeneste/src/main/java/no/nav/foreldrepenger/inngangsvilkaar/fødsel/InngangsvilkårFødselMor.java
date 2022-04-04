package no.nav.foreldrepenger.inngangsvilkaar.fødsel;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel.FødselsvilkårMor;

/**
 * Adapter for å evaluere fødselsvilkåret.
 */
@ApplicationScoped
@VilkårTypeRef(VilkårType.FØDSELSVILKÅRET_MOR)
public class InngangsvilkårFødselMor implements Inngangsvilkår {

    private InngangsvilkårOversetter inngangsvilkårOversetter;

    InngangsvilkårFødselMor() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårFødselMor(InngangsvilkårOversetter inngangsvilkårOversetter) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        var grunnlag = inngangsvilkårOversetter.oversettTilRegelModellFødsel(ref);

        var evaluation = new FødselsvilkårMor().evaluer(grunnlag);

        return InngangsvilkårOversetter.tilVilkårData(VilkårType.FØDSELSVILKÅRET_MOR, evaluation, grunnlag);
    }
}
