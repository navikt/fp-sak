package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class OpptjeningAktivitetVurderingVilkår implements OpptjeningAktivitetVurdering {

    private AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening;
    private AksjonspunktutlederForVurderBekreftetOpptjening vurderBekreftetOpptjening;

    public OpptjeningAktivitetVurderingVilkår(AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening, AksjonspunktutlederForVurderBekreftetOpptjening vurderBekreftetOpptjening) {
        this.vurderOppgittOpptjening = vurderOppgittOpptjening;
        this.vurderBekreftetOpptjening = vurderBekreftetOpptjening;
    }

    @Override
    public VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                         BehandlingReferanse behandlingReferanse,
                                         Yrkesaktivitet overstyrtAktivitet,
                                         InntektArbeidYtelseGrunnlag iayGrunnlag,
                                         boolean harVærtSaksbehandlet) {
        return vurderStatus(type, behandlingReferanse, null, overstyrtAktivitet, iayGrunnlag, harVærtSaksbehandlet);
    }

    @Override
    public VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                         BehandlingReferanse behandlingReferanse,
                                         Yrkesaktivitet registerAktivitet,
                                         Yrkesaktivitet overstyrtAktivitet,
                                         InntektArbeidYtelseGrunnlag iayGrunnlag,
                                         boolean harVærtSaksbehandlet) {
        if (OpptjeningAktivitetType.ANNEN_OPPTJENING.contains(type)) {
            return vurderAnnenOpptjening(overstyrtAktivitet);
        } else if (OpptjeningAktivitetType.NÆRING.equals(type)) {
            Skjæringstidspunkt skjæringstidspunkt = behandlingReferanse.getSkjæringstidspunkt();
            return vurderNæring(behandlingReferanse.getBehandlingId(), behandlingReferanse.getAktørId(), iayGrunnlag, overstyrtAktivitet, skjæringstidspunkt);
        } else if (OpptjeningAktivitetType.ARBEID.equals(type)) {
            var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), (Yrkesaktivitet) null);
            return vurderArbeid(filter, registerAktivitet, overstyrtAktivitet, behandlingReferanse.getBehandlingId());
        }
        return VurderingsStatus.TIL_VURDERING;
    }

    private VurderingsStatus vurderArbeid(YrkesaktivitetFilter filter, Yrkesaktivitet registerAktivitet, Yrkesaktivitet overstyrtAktivitet, Long behandlingId) {
        if (vurderBekreftetOpptjening.girAksjonspunktForArbeidsforhold(filter, behandlingId, registerAktivitet, overstyrtAktivitet)) {
            if (overstyrtAktivitet != null) {
                return VurderingsStatus.FERDIG_VURDERT_GODKJENT;
            }
            return VurderingsStatus.FERDIG_VURDERT_UNDERKJENT;
        }
        return VurderingsStatus.TIL_VURDERING;
    }

    private VurderingsStatus vurderAnnenOpptjening(Yrkesaktivitet overstyrtAktivitet) {
        if (overstyrtAktivitet != null) {
            return VurderingsStatus.FERDIG_VURDERT_GODKJENT;
        }
        return VurderingsStatus.FERDIG_VURDERT_UNDERKJENT;
    }

    private VurderingsStatus vurderNæring(Long behandlingId, AktørId aktørId, InntektArbeidYtelseGrunnlag iayg, Yrkesaktivitet overstyrtAktivitet, Skjæringstidspunkt skjæringstidspunkt) {
        if (vurderOppgittOpptjening.girAksjonspunktForOppgittNæring(behandlingId, aktørId, iayg, skjæringstidspunkt)) {
            if (overstyrtAktivitet != null) {
                return VurderingsStatus.FERDIG_VURDERT_GODKJENT;
            }
            return VurderingsStatus.FERDIG_VURDERT_UNDERKJENT;
        }
        return VurderingsStatus.TIL_VURDERING;
    }
}
