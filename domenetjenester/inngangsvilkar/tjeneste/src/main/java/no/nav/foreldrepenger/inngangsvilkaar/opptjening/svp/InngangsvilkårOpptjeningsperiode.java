package no.nav.foreldrepenger.inngangsvilkaar.opptjening.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårTypeKoder;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsperiodeVilkårTjeneste;

@ApplicationScoped
@VilkårTypeRef(VilkårTypeKoder.FP_VK_21)
@FagsakYtelseTypeRef("SVP")
public class InngangsvilkårOpptjeningsperiode implements Inngangsvilkår {

    private OpptjeningsperiodeVilkårTjeneste opptjeningsperiodeVilkårTjeneste;

    InngangsvilkårOpptjeningsperiode() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårOpptjeningsperiode(@FagsakYtelseTypeRef("SVP") OpptjeningsperiodeVilkårTjeneste opptjeningsperiodeVilkårTjeneste) {
        this.opptjeningsperiodeVilkårTjeneste = opptjeningsperiodeVilkårTjeneste;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        return opptjeningsperiodeVilkårTjeneste.vurderOpptjeningsperiodeVilkår(ref, ref.getSkjæringstidspunkt().getFørsteUttaksdato());
    }
}
