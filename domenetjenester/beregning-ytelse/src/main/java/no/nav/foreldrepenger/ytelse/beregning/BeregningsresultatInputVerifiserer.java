package no.nav.foreldrepenger.ytelse.beregning;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;

public final class BeregningsresultatInputVerifiserer {

    private BeregningsresultatInputVerifiserer() {
        // Skjuler default
    }

    public static void verifiserAndelerIUttakLiggerIBeregning(BeregningsresultatRegelmodell input) {
        List<BeregningsgrunnlagPrStatus> bGAndeler = hentBGAndeler(input);
        List<UttakAktivitet> uttakAndeler = hentUttakAndeler(input);
        uttakAndeler.forEach(uttakAndel -> verifiserUttak(uttakAndel, bGAndeler));
    }

    public static void verifiserAlleAndelerIBeregningErIUttak(BeregningsresultatRegelmodell input) {
        List<BeregningsgrunnlagPrStatus> bGAndeler = hentBGAndeler(input);
        List<UttakAktivitet> uttakAndeler = hentUttakAndeler(input);
        bGAndeler.forEach(bgAndel -> verifiserAtAndelerMatcher(bgAndel, uttakAndeler));
    }

    private static List<UttakAktivitet> hentUttakAndeler(BeregningsresultatRegelmodell input) {
        return input.getUttakResultat().getUttakResultatPerioder().stream().map(UttakResultatPeriode::getUttakAktiviteter).flatMap(Collection::stream).collect(Collectors.toList());
    }

    private static List<BeregningsgrunnlagPrStatus> hentBGAndeler(BeregningsresultatRegelmodell input) {
        return input.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().stream().map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatus).flatMap(Collection::stream).collect(Collectors.toList());
    }

    private static void verifiserUttak(UttakAktivitet uttakAndel, List<BeregningsgrunnlagPrStatus> bGAndeler) {
        if (uttakAndel.getAktivitetStatus().equals(AktivitetStatus.ATFL)) {
            finnMatchendeBGArbeidsforhold(uttakAndel, bGAndeler);
        } else {
            finnMatchendeBGAndel(uttakAndel, bGAndeler);
        }
    }

    private static void finnMatchendeBGAndel(UttakAktivitet uttakAndel, List<BeregningsgrunnlagPrStatus> bGAndeler) {
        Optional<BeregningsgrunnlagPrStatus> matchendeBGAndel = bGAndeler.stream()
            .filter(bgAndel -> BeregningsgrunnlagUttakArbeidsforholdMatcher.matcherGenerellAndel(bgAndel, uttakAndel))
            .findFirst();
        if (matchendeBGAndel.isEmpty()) {
            throw BeregningsresultatVerifisererFeil.FEILFACTORY.verifiserAtUttakAndelHarMatchendeBeregningsgrunnlagAndel(uttakAndel.toString(), bGAndeler.toString()).toException();
        }
    }

    private static void finnMatchendeBGArbeidsforhold(UttakAktivitet uttakAndel, List<BeregningsgrunnlagPrStatus> bGAndeler) {
        List<BeregningsgrunnlagPrArbeidsforhold> bgArbeidsforhold = bGAndeler.stream()
            .filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.ATFL))
            .map(BeregningsgrunnlagPrStatus::getArbeidsforhold)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        Optional<BeregningsgrunnlagPrArbeidsforhold> matchetBGArbfor = bgArbeidsforhold.stream()
            .filter(a -> BeregningsgrunnlagUttakArbeidsforholdMatcher.matcherArbeidsforhold(uttakAndel.getArbeidsforhold(), a.getArbeidsforhold()))
            .findFirst();
        if (matchetBGArbfor.isEmpty()) {
            throw BeregningsresultatVerifisererFeil.FEILFACTORY.verifiserAtUttakAndelHarMatchendeBeregningsgrunnlagAndel(uttakAndel.toString(), bGAndeler.toString()).toException();
        }
    }

    private static void verifiserAtAndelerMatcher(BeregningsgrunnlagPrStatus bgAndel, List<UttakAktivitet> uttakAndeler) {
        if (bgAndel.getAktivitetStatus().equals(AktivitetStatus.ATFL)) {
            matchArbeidsforhold(bgAndel.getArbeidsforhold(), uttakAndeler);
        } else {
            matchAndel(bgAndel, uttakAndeler);
        }
    }

    private static void matchAndel(BeregningsgrunnlagPrStatus bgAndel, List<UttakAktivitet> uttakAndeler) {
        Optional<UttakAktivitet> matchetUttaksandel = uttakAndeler.stream()
            .filter(uttakAndel -> BeregningsgrunnlagUttakArbeidsforholdMatcher.matcherGenerellAndel(bgAndel, uttakAndel))
            .findFirst();
        if (matchetUttaksandel.isEmpty()) {
            throw BeregningsresultatVerifisererFeil.FEILFACTORY.verifiserAtBeregningsgrunnlagAndelHarMatchendeUttakandel(bgAndel.toString(), uttakAndeler.toString()).toException();
        }
    }

    private static void matchArbeidsforhold(List<BeregningsgrunnlagPrArbeidsforhold> arbeidsforhold, List<UttakAktivitet> uttakAndeler) {
        arbeidsforhold.forEach(arbfor -> {
            Optional<UttakAktivitet> mathendeUttaksaktivitet = finnMatchendeUttakArbeidsforhold(arbfor, uttakAndeler);
            if (mathendeUttaksaktivitet.isEmpty()) {
                throw BeregningsresultatVerifisererFeil.FEILFACTORY.verifiserAtBeregningsgrunnlagAndelHarMatchendeUttakandel(arbfor.toString(), uttakAndeler.toString()).toException();
            }
        });
    }

    private static Optional<UttakAktivitet> finnMatchendeUttakArbeidsforhold(BeregningsgrunnlagPrArbeidsforhold beregningsgrunnlagArbeidsforhold, List<UttakAktivitet> uttakAndeler) {
        return uttakAndeler.stream()
            .filter(ua -> BeregningsgrunnlagUttakArbeidsforholdMatcher.matcherArbeidsforhold(beregningsgrunnlagArbeidsforhold.getArbeidsforhold(), ua.getArbeidsforhold()))
            .findFirst();
    }

}
