package no.nav.foreldrepenger.inngangsvilkaar.opptjening.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsVilkårTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@VilkårTypeRef(VilkårType.OPPTJENINGSVILKÅRET)
public class InngangsvilkårOpptjening implements Inngangsvilkår {

    private OpptjeningsVilkårTjeneste opptjeningsVilkårTjeneste;

    InngangsvilkårOpptjening() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårOpptjening(@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) OpptjeningsVilkårTjeneste opptjeningsVilkårTjeneste) {
        this.opptjeningsVilkårTjeneste = opptjeningsVilkårTjeneste;

    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        // returner egen output i tillegg for senere lagring
        return opptjeningsVilkårTjeneste.vurderOpptjeningsVilkår(ref);
    }

}
