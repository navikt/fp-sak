package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.RegelOrkestrerer;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

import java.util.Set;

@ApplicationScoped
public class InngangsvilkårFellesTjeneste {
    private RegelOrkestrerer regelOrkestrerer;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    InngangsvilkårFellesTjeneste() {
        // CDI
    }

    @Inject
    public InngangsvilkårFellesTjeneste(RegelOrkestrerer regelOrkestrerer, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.regelOrkestrerer = regelOrkestrerer;
    }

    RegelResultat vurderInngangsvilkår(Set<VilkårType> vilkårHåndtertAvSteg, Behandling behandling, BehandlingReferanse ref) {
        return regelOrkestrerer.vurderInngangsvilkår(vilkårHåndtertAvSteg, behandling, ref);
    }

    Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        return skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
    }
}
