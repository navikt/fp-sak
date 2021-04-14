package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;

public class OpptjeningAktivitetVurderingAksjonspunkt implements OpptjeningAktivitetVurdering {

    private AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening;
    private AksjonspunktutlederForVurderBekreftetOpptjening vurderBekreftetOpptjening;

    public OpptjeningAktivitetVurderingAksjonspunkt(AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening,
            AksjonspunktutlederForVurderBekreftetOpptjening vurderBekreftetOpptjening) {
        this.vurderOppgittOpptjening = vurderOppgittOpptjening;
        this.vurderBekreftetOpptjening = vurderBekreftetOpptjening;
    }

    @Override
    public boolean skalInkludereAkkumulertFrilans() {
        return true;
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

        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), (Yrkesaktivitet) null);
        if (OpptjeningAktivitetType.ANNEN_OPPTJENING.contains(type)) {
            return vurderAnnenOpptjening(overstyrtAktivitet, harVærtSaksbehandlet);
        }
        if (OpptjeningAktivitetType.NÆRING.equals(type)) {
            var skjæringstidspunkt = behandlingReferanse.getSkjæringstidspunkt();
            return vurderNæring(behandlingReferanse, overstyrtAktivitet, iayGrunnlag, skjæringstidspunkt, harVærtSaksbehandlet);
        }
        if (OpptjeningAktivitetType.ARBEID.equals(type)) {
            return vurderArbeid(filter, registerAktivitet, overstyrtAktivitet, harVærtSaksbehandlet, behandlingReferanse);
        }
        return VurderingsStatus.GODKJENT;
    }

    /**
     * @param registerAktivitet    aktiviteten
     * @param overstyrtAktivitet   aktiviteten
     * @param harVærtSaksbehandlet har saksbehandler tatt stilling til dette
     * @param behandlingReferanse
     * @return vurderingsstatus
     */
    private VurderingsStatus vurderArbeid(YrkesaktivitetFilter filter, Yrkesaktivitet registerAktivitet, Yrkesaktivitet overstyrtAktivitet,
            boolean harVærtSaksbehandlet, BehandlingReferanse behandlingReferanse) {
        if (vurderBekreftetOpptjening.girAksjonspunktForArbeidsforhold(filter, behandlingReferanse.getBehandlingId(), registerAktivitet,
                overstyrtAktivitet)) {
            if (overstyrtAktivitet != null) {
                return VurderingsStatus.GODKJENT;
            }
            if (harVærtSaksbehandlet) {
                return VurderingsStatus.UNDERKJENT;
            }
            return VurderingsStatus.TIL_VURDERING;
        }
        return VurderingsStatus.GODKJENT;
    }

    /**
     * @param overstyrtAktivitet   aktiviteten
     * @param harVærtSaksbehandlet har saksbehandler tatt stilling til dette
     * @return vurderingsstatus
     */
    private VurderingsStatus vurderAnnenOpptjening(Yrkesaktivitet overstyrtAktivitet, boolean harVærtSaksbehandlet) {
        if (overstyrtAktivitet != null) {
            return VurderingsStatus.GODKJENT;
        }
        if (harVærtSaksbehandlet) {
            return VurderingsStatus.UNDERKJENT;
        }
        return VurderingsStatus.TIL_VURDERING;
    }

    /**
     * @param behandling           behandlingen
     * @param overstyrtAktivitet   aktiviteten
     * @param harVærtSaksbehandlet har saksbehandler tatt stilling til dette
     * @return vurderingsstatus
     */
    private VurderingsStatus vurderNæring(BehandlingReferanse behandlingReferanse, Yrkesaktivitet overstyrtAktivitet,
            InntektArbeidYtelseGrunnlag iayGrunnlag, Skjæringstidspunkt skjæringstidspunkt, boolean harVærtSaksbehandlet) {
        if (vurderOppgittOpptjening.girAksjonspunktForOppgittNæring(behandlingReferanse.getBehandlingId(), behandlingReferanse.getAktørId(),
                iayGrunnlag, skjæringstidspunkt)) {
            if (overstyrtAktivitet != null) {
                return VurderingsStatus.GODKJENT;
            }
            if (harVærtSaksbehandlet) {
                return VurderingsStatus.UNDERKJENT;
            }
            return VurderingsStatus.TIL_VURDERING;
        }
        return VurderingsStatus.GODKJENT;
    }
}
