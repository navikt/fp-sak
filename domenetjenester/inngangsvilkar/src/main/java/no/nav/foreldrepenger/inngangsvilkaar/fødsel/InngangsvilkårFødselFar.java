package no.nav.foreldrepenger.inngangsvilkaar.fødsel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultatOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.InngangsvilkårRegler;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

/**
 * Adapter for å evaluere fødselsvilkåret for far.
 */
@ApplicationScoped
@VilkårTypeRef(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR)
class InngangsvilkårFødselFar implements Inngangsvilkår {
    private FødselsvilkårOversetter fødselsvilkårOversetter;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    InngangsvilkårFødselFar() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårFødselFar(FødselsvilkårOversetter fødselsvilkårOversetter, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.fødselsvilkårOversetter = fødselsvilkårOversetter;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }


    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        var utenMinsterett = skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId()).utenMinsterett();
        var grunnlag = fødselsvilkårOversetter.oversettTilRegelModellFødsel(ref, utenMinsterett);

        var resultat = InngangsvilkårRegler.fødsel(RegelSøkerRolle.FARA, grunnlag);

        return RegelResultatOversetter.oversett(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR, resultat);
    }
}
