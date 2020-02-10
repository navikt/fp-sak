package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.fordel;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class NyttArbeidsforholdTjeneste {

    private NyttArbeidsforholdTjeneste() {
        // SKjuler default
    }

    public static boolean erNyttArbeidsforhold(BeregningsgrunnlagPrStatusOgAndel andel, BeregningAktivitetAggregatEntitet beregningAktivitetAggregat) {
        if (andel.getBgAndelArbeidsforhold().isEmpty()) {
            return false;
        }
        LocalDate skjæringstidspunkt = andel.getBeregningsgrunnlagPeriode().getBeregningsgrunnlag().getSkjæringstidspunkt();
        BGAndelArbeidsforhold arbeidsforhold = andel.getBgAndelArbeidsforhold().get();
        List<BeregningAktivitetEntitet> beregningAktiviteter = beregningAktivitetAggregat.getBeregningAktiviteter();
        return beregningAktiviteter.stream()
            .filter(beregningAktivitet -> slutterPåEllerEtterSTP(skjæringstidspunkt, beregningAktivitet))
            .filter(beregningAktivitet -> starterFørSTP(skjæringstidspunkt, beregningAktivitet))
            .noneMatch(
                beregningAktivitet -> matcherArbeidsgiver(arbeidsforhold, beregningAktivitet) && matcherReferanse(arbeidsforhold, beregningAktivitet));
    }

    private static boolean matcherArbeidsgiver(BGAndelArbeidsforhold andel, BeregningAktivitetEntitet aktivitet) {
        return Objects.equals(andel.getArbeidsgiver(), aktivitet.getArbeidsgiver());
    }

    private static boolean matcherReferanse(BGAndelArbeidsforhold andel, BeregningAktivitetEntitet aktivitet) {
        String andelRef = andel.getArbeidsforholdRef().getReferanse();
        String aktivitetRef = aktivitet.getArbeidsforholdRef().getReferanse();
        return Objects.equals(andelRef, aktivitetRef);
    }

    private static boolean starterFørSTP(LocalDate skjæringstidspunkt, BeregningAktivitetEntitet beregningAktivitet) {
        return !beregningAktivitet.getPeriode().getFomDato().isAfter(skjæringstidspunkt.minusDays(1));
    }

    private static boolean slutterPåEllerEtterSTP(LocalDate skjæringstidspunkt, BeregningAktivitetEntitet beregningAktivitet) {
        return !beregningAktivitet.getPeriode().getTomDato().isBefore(skjæringstidspunkt.minusDays(1));
    }
}
