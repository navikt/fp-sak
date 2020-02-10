package no.nav.foreldrepenger.inngangsvilkaar.opptjening.fp;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningInntektPeriode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsVilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsgrunnlagAdapter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsgrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
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
        Long behandlingId = behandlingReferanse.getBehandlingId();
        AktørId aktørId = behandlingReferanse.getAktørId();
        LocalDate skjæringstidspunkt = behandlingReferanse.getUtledetSkjæringstidspunkt();

        List<OpptjeningAktivitetPeriode> relevanteOpptjeningAktiveter = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(behandlingReferanse);
        List<OpptjeningInntektPeriode> relevanteOpptjeningInntekter = opptjeningTjeneste.hentRelevanteOpptjeningInntekterForVilkårVurdering(behandlingId, aktørId, skjæringstidspunkt);
        Opptjening opptjening = opptjeningTjeneste.hentOpptjening(behandlingId);

        LocalDate behandlingstidspunkt = FPDateUtil.iDag(); // TODO (FC): Avklar hva denne bør være

        Opptjeningsgrunnlag grunnlag = new OpptjeningsgrunnlagAdapter(behandlingstidspunkt, opptjening.getFom(),
            opptjening.getTom())
            .mapTilGrunnlag(relevanteOpptjeningAktiveter, relevanteOpptjeningInntekter);

        // returner egen output i tillegg for senere lagring
        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new OpptjeningsvilkårForeldrepenger().evaluer(grunnlag, output);

        VilkårData vilkårData = inngangsvilkårOversetter.tilVilkårData(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag);
        vilkårData.setEkstraVilkårresultat(output);

        return vilkårData;
    }
}
