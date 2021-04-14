package no.nav.foreldrepenger.ytelse.beregning;

import java.util.Objects;

import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;

public final class BeregningsgrunnlagUttakArbeidsforholdMatcher {

    private BeregningsgrunnlagUttakArbeidsforholdMatcher() {
        // Skjuler default
    }

    public static boolean matcherArbeidsforhold(Arbeidsforhold arbeidsforholdUttak, Arbeidsforhold arbeidsforholdBeregning) {
        if (arbeidsforholdBeregning == null || arbeidsforholdUttak == null) {
            // begge må være null for at de skal være like
            return Objects.equals(arbeidsforholdBeregning, arbeidsforholdUttak);
        }
        var bgRef = InternArbeidsforholdRef.ref(arbeidsforholdBeregning.getArbeidsforholdId());
        var uttakRef = InternArbeidsforholdRef.ref(arbeidsforholdUttak.getArbeidsforholdId());
        return Objects.equals(arbeidsforholdBeregning.erFrilanser(), arbeidsforholdUttak.erFrilanser())
            && Objects.equals(arbeidsforholdBeregning.getIdentifikator(), arbeidsforholdUttak.getIdentifikator())
            && bgRef.gjelderFor(uttakRef);
    }

    public static boolean matcherGenerellAndel(BeregningsgrunnlagPrStatus beregningsgrunnlagPrStatus, UttakAktivitet aktivitet) {
        return aktivitet.getAktivitetStatus().equals(beregningsgrunnlagPrStatus.getAktivitetStatus())
            || (aktivitet.getAktivitetStatus().equals(AktivitetStatus.ANNET) && !beregningsgrunnlagPrStatus.getAktivitetStatus().erGraderbar());
    }
}
