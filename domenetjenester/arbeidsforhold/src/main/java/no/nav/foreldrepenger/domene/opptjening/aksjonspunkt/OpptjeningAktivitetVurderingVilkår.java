package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class OpptjeningAktivitetVurderingVilkår implements OpptjeningAktivitetVurdering {

    private final AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening;
    private final AksjonspunktutlederForVurderBekreftetOpptjening vurderBekreftetOpptjening;

    public OpptjeningAktivitetVurderingVilkår(AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening,
            AksjonspunktutlederForVurderBekreftetOpptjening vurderBekreftetOpptjening) {
        this.vurderOppgittOpptjening = vurderOppgittOpptjening;
        this.vurderBekreftetOpptjening = vurderBekreftetOpptjening;
    }

    @Override
    public boolean skalInkludereDetaljertFrilansOppdrag() {
        return true;
    }

    @Override
    public VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
            BehandlingReferanse behandlingReferanse, Skjæringstidspunkt skjæringstidspunkt,
            Yrkesaktivitet overstyrtAktivitet,
            InntektArbeidYtelseGrunnlag iayGrunnlag,
            boolean harVærtSaksbehandlet) {
        return vurderStatus(type, behandlingReferanse, skjæringstidspunkt, overstyrtAktivitet, iayGrunnlag, harVærtSaksbehandlet, null, null);
    }

    @Override
    public VurderingsStatus vurderStatus(OpptjeningAktivitetType type,
                                         BehandlingReferanse behandlingReferanse,
                                         Skjæringstidspunkt skjæringstidspunkt,
                                         Yrkesaktivitet overstyrtAktivitet,
                                         InntektArbeidYtelseGrunnlag iayGrunnlag,
                                         boolean harVærtSaksbehandlet,
                                         Yrkesaktivitet registerAktivitet,
                                         AktivitetsAvtale ansettelsesPeriode) {
        if (OpptjeningAktivitetType.ANNEN_OPPTJENING.contains(type)) {
            return vurderAnnenOpptjening(overstyrtAktivitet);
        }
        if (OpptjeningAktivitetType.NÆRING.equals(type)) {
            return vurderNæring(behandlingReferanse.behandlingId(), behandlingReferanse.aktørId(), iayGrunnlag, overstyrtAktivitet,
                skjæringstidspunkt);
        }
        if (OpptjeningAktivitetType.ARBEID.equals(type)) {
            var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), (Yrkesaktivitet) null);
            return vurderArbeid(filter, registerAktivitet, overstyrtAktivitet, behandlingReferanse.behandlingId(), ansettelsesPeriode);
        }
        return VurderingsStatus.TIL_VURDERING;
    }

    private VurderingsStatus vurderArbeid(YrkesaktivitetFilter filter, Yrkesaktivitet registerAktivitet, Yrkesaktivitet overstyrtAktivitet,
                                          Long behandlingId, AktivitetsAvtale ansettelsesPeriode) {
        if (vurderBekreftetOpptjening.girAksjonspunktForAnsettelsesperiode(filter, behandlingId, registerAktivitet, overstyrtAktivitet, ansettelsesPeriode)) {
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

    private VurderingsStatus vurderNæring(Long behandlingId, AktørId aktørId, InntektArbeidYtelseGrunnlag iayg, Yrkesaktivitet overstyrtAktivitet,
            Skjæringstidspunkt skjæringstidspunkt) {
        if (vurderOppgittOpptjening.girAksjonspunktForOppgittNæring(behandlingId, aktørId, iayg, skjæringstidspunkt)) {
            if (overstyrtAktivitet != null) {
                return VurderingsStatus.FERDIG_VURDERT_GODKJENT;
            }
            return VurderingsStatus.FERDIG_VURDERT_UNDERKJENT;
        }
        return VurderingsStatus.TIL_VURDERING;
    }
}
