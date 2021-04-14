package no.nav.foreldrepenger.inngangsvilkaar.opptjening.svp;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsVilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsgrunnlagAdapter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.svp.OpptjeningsvilkårSvangerskapspenger;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class OpptjeningsVilkårTjenesteImpl implements OpptjeningsVilkårTjeneste {
    private OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste;
    private InngangsvilkårOversetter inngangsvilkårOversetter;

    public OpptjeningsVilkårTjenesteImpl() {
    }

    @Inject
    public OpptjeningsVilkårTjenesteImpl(InngangsvilkårOversetter inngangsvilkårOversetter,
                                        OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
        this.opptjeningTjeneste = opptjeningTjeneste;
    }


    @Override
    public VilkårData vurderOpptjeningsVilkår(BehandlingReferanse behandlingReferanse) {
        var behandlingId = behandlingReferanse.getBehandlingId();
        var aktørId = behandlingReferanse.getAktørId();
        var skjæringstidspunkt = behandlingReferanse.getUtledetSkjæringstidspunkt();

        var relevanteOpptjeningAktiveter = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(behandlingReferanse);
        var relevanteOpptjeningInntekter = opptjeningTjeneste.hentRelevanteOpptjeningInntekterForVilkårVurdering(behandlingId, aktørId, skjæringstidspunkt);
        var opptjening = opptjeningTjeneste.hentOpptjening(behandlingId);

        var behandlingstidspunkt = LocalDate.now();

        var grunnlag = new OpptjeningsgrunnlagAdapter(behandlingstidspunkt, opptjening.getFom(),
            opptjening.getTom())
            .mapTilGrunnlag(relevanteOpptjeningAktiveter, relevanteOpptjeningInntekter);

        //TODO(OJR) overstyrer konfig for fp... burde blitt flyttet ut til konfig verdier.. både for FP og for SVP???
        grunnlag.setMinsteAntallDagerGodkjent(28);
        grunnlag.setMinsteAntallMånederGodkjent(0);
        //TODO(OJR) denne burde kanskje endres til false i en revurdering-kontekts i etterkant?
        grunnlag.setSkalGodkjenneBasertPåAntatt(true);

        // returner egen output i tillegg for senere lagring
        var output = new OpptjeningsvilkårResultat();
        var evaluation = new OpptjeningsvilkårSvangerskapspenger().evaluer(grunnlag, output);

        var vilkårData = inngangsvilkårOversetter.tilVilkårData(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);
        vilkårData.setEkstraVilkårresultat(output);

        return vilkårData;
    }
}
