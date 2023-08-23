package no.nav.foreldrepenger.inngangsvilkaar.adopsjon;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultatOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.InngangsvilkårRegler;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelYtelse;

@ApplicationScoped
@VilkårTypeRef(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER)
public class InngangsvilkårForeldrepengerAdopsjon implements Inngangsvilkår {

    private AdopsjonsvilkårOversetter adopsjonsvilkårOversetter;

    public InngangsvilkårForeldrepengerAdopsjon() {
    }

    @Inject
    public InngangsvilkårForeldrepengerAdopsjon(AdopsjonsvilkårOversetter adopsjonsvilkårOversetter) {
        this.adopsjonsvilkårOversetter = adopsjonsvilkårOversetter;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        var grunnlag = adopsjonsvilkårOversetter.oversettTilRegelModellAdopsjon(ref);

        var resultat = InngangsvilkårRegler.adopsjon(RegelYtelse.FORELDREPENGER, grunnlag);

        return RegelResultatOversetter.oversett(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER, resultat);
    }
}
