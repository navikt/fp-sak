package no.nav.foreldrepenger.inngangsvilkaar.fødsel;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultatOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.InngangsvilkårRegler;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;

/**
 * Adapter for å evaluere fødselsvilkåret.
 */
@ApplicationScoped
@VilkårTypeRef(VilkårType.FØDSELSVILKÅRET_MOR)
public class InngangsvilkårFødselMor implements Inngangsvilkår {

    private FødselsvilkårOversetter fødselsvilkårOversetter;

    InngangsvilkårFødselMor() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårFødselMor(FødselsvilkårOversetter fødselsvilkårOversetter) {
        this.fødselsvilkårOversetter = fødselsvilkårOversetter;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        var grunnlag = fødselsvilkårOversetter.oversettTilRegelModellFødsel(ref);

        var resultat = InngangsvilkårRegler.fødsel(RegelSøkerRolle.MORA, grunnlag);

        return RegelResultatOversetter.oversett(VilkårType.FØDSELSVILKÅRET_MOR, resultat);
    }
}
