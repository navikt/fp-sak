package no.nav.foreldrepenger.inngangsvilkaar.søknad;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.søknadsfrist.Søknadsfristvilkår;

@ApplicationScoped
@VilkårTypeRef(VilkårType.SØKNADSFRISTVILKÅRET)
public class InngangsvilkårEngangsstønadSøknadsfrist implements Inngangsvilkår {

    private InngangsvilkårOversetter inngangsvilkårOversetter;

    InngangsvilkårEngangsstønadSøknadsfrist() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårEngangsstønadSøknadsfrist(InngangsvilkårOversetter inngangsvilkårOversetter) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse behandling) {
        var grunnlag = inngangsvilkårOversetter.oversettTilRegelModellSøknad(behandling);
        var vilkaarResultat = new Søknadsfristvilkår().evaluer(grunnlag);
        return InngangsvilkårOversetter.tilVilkårData(VilkårType.SØKNADSFRISTVILKÅRET, vilkaarResultat, grunnlag);
    }

}
