package no.nav.foreldrepenger.inngangsvilkaar.opptjening.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsperiodeVilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.svp.RegelFastsettOpptjeningsperiode;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class OpptjeningsperiodeVilkårTjenesteImpl implements OpptjeningsperiodeVilkårTjeneste {

    @Inject
    public OpptjeningsperiodeVilkårTjenesteImpl() {
    }

    @Override
    public VilkårData vurderOpptjeningsperiodeVilkår(BehandlingReferanse behandlingReferanse) {
        final var førsteUttaksdato = behandlingReferanse.getSkjæringstidspunkt().getFørsteUttaksdato();
        var grunnlag = new OpptjeningsperiodeGrunnlag(
            FagsakÅrsak.SVANGERSKAP,
            RegelSøkerRolle.MORA,
            førsteUttaksdato,
            null,
            null,
            null,
            null);

        final var data = new OpptjeningsPeriode();
        var evaluation = new RegelFastsettOpptjeningsperiode().evaluer(grunnlag, data);

        var resultat = InngangsvilkårOversetter.tilVilkårData(VilkårType.OPPTJENINGSPERIODEVILKÅR, evaluation, grunnlag, data);
        return resultat;
    }
}
