package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakAktivitet;

import java.util.Objects;

public final class BeregningsgrunnlagUttakArbeidsforholdMatcher {

    private BeregningsgrunnlagUttakArbeidsforholdMatcher() {
        // Skjuler default
    }

    public static boolean matcherArbeidsforhold(Arbeidsforhold arbeidsforholdUttak, Arbeidsforhold arbeidsforholdBeregning) {
        if (arbeidsforholdBeregning == null || arbeidsforholdUttak == null) {
            // begge må være null for at de skal være like
            return Objects.equals(arbeidsforholdBeregning, arbeidsforholdUttak);
        }
        var bgRef = InternArbeidsforholdRef.ref(arbeidsforholdBeregning.arbeidsforholdId());
        var uttakRef = InternArbeidsforholdRef.ref(arbeidsforholdUttak.arbeidsforholdId());
        return Objects.equals(arbeidsforholdBeregning.frilanser(), arbeidsforholdUttak.frilanser())
            && Objects.equals(arbeidsforholdBeregning.identifikator(), arbeidsforholdUttak.identifikator())
            && bgRef.gjelderFor(uttakRef);
    }

    public static boolean matcherGenerellAndel(BeregningsgrunnlagPrStatus beregningsgrunnlagPrStatus, UttakAktivitet aktivitet) {
        return aktivitet.aktivitetStatus().equals(beregningsgrunnlagPrStatus.aktivitetStatus())
            || aktivitet.aktivitetStatus().equals(AktivitetStatus.ANNET) && !beregningsgrunnlagPrStatus.aktivitetStatus().erGraderbar();
    }
}
