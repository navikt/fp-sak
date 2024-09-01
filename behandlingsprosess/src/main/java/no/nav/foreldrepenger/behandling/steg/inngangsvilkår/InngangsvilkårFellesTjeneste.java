package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.RegelOrkestrerer;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;

@ApplicationScoped
public class InngangsvilkårFellesTjeneste {
    private RegelOrkestrerer regelOrkestrerer;

    InngangsvilkårFellesTjeneste() {
        // CDI
    }

    @Inject
    public InngangsvilkårFellesTjeneste(RegelOrkestrerer regelOrkestrerer) {
        this.regelOrkestrerer = regelOrkestrerer;
    }

    RegelResultat vurderInngangsvilkår(Set<VilkårType> vilkårHåndtertAvSteg, Behandling behandling, BehandlingReferanse ref) {
        return regelOrkestrerer.vurderInngangsvilkår(vilkårHåndtertAvSteg, behandling, ref);
    }
}
