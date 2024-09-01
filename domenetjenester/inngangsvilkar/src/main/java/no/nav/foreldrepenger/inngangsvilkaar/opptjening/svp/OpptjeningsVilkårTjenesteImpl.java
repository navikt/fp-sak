package no.nav.foreldrepenger.inngangsvilkaar.opptjening.svp;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultatOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsVilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.opptjening.OpptjeningsgrunnlagAdapter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.InngangsvilkårRegler;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelYtelse;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
public class OpptjeningsVilkårTjenesteImpl implements OpptjeningsVilkårTjeneste {
    private OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public OpptjeningsVilkårTjenesteImpl() {
    }

    @Inject
    public OpptjeningsVilkårTjenesteImpl(OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.opptjeningTjeneste = opptjeningTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }


    @Override
    public VilkårData vurderOpptjeningsVilkår(BehandlingReferanse behandlingReferanse) {
        var behandlingId = behandlingReferanse.behandlingId();
        var aktørId = behandlingReferanse.aktørId();
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();

        var relevanteOpptjeningAktiveter = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(behandlingReferanse, stp);
        var relevanteOpptjeningInntekter = opptjeningTjeneste.hentRelevanteOpptjeningInntekterForVilkårVurdering(behandlingId, aktørId, skjæringstidspunkt);
        var opptjening = opptjeningTjeneste.hentOpptjening(behandlingId);

        var behandlingstidspunkt = LocalDate.now();

        var grunnlag = new OpptjeningsgrunnlagAdapter(behandlingstidspunkt, opptjening.getFom(),
            opptjening.getTom())
            .mapTilGrunnlag(relevanteOpptjeningAktiveter, relevanteOpptjeningInntekter);

        var resultat = InngangsvilkårRegler.opptjening(RegelYtelse.SVANGERSKAPSPENGER, grunnlag);

        return RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSVILKÅRET, resultat);
    }
}
