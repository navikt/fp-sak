package no.nav.foreldrepenger.inngangsvilkaar.adopsjon;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.AdopsjonsvilkårForeldrepenger;

@ApplicationScoped
@VilkårTypeRef(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER)
public class InngangsvilkårForeldrepengerAdopsjon implements Inngangsvilkår {

    private InngangsvilkårOversetter inngangsvilkårOversetter;

    public InngangsvilkårForeldrepengerAdopsjon() {
    }

    @Inject
    public InngangsvilkårForeldrepengerAdopsjon(InngangsvilkårOversetter inngangsvilkårOversetter) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        var grunnlag = inngangsvilkårOversetter.oversettTilRegelModellAdopsjon(ref);

        var evaluation = new AdopsjonsvilkårForeldrepenger().evaluer(grunnlag);

        return InngangsvilkårOversetter.tilVilkårData(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER, evaluation, grunnlag);
    }
}
