package no.nav.foreldrepenger.ytelse.beregning;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
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
import no.nav.vedtak.exception.TekniskException;

public final class BeregningsresultatInputVerifiserer {

    private BeregningsresultatInputVerifiserer() {
        // Skjuler default
    }

    public static void verifiserAndelerIUttakLiggerIBeregning(BeregningsresultatRegelmodell input) {
        if (brukerHarUttak(input)) {
            var bGAndeler = hentBGAndeler(input);
            var uttakAndeler = hentUttakAndeler(input);
            uttakAndeler.forEach(uttakAndel -> verifiserUttak(uttakAndel, bGAndeler));
        }
    }

    private static boolean brukerHarUttak(BeregningsresultatRegelmodell input) {
        return !input.getUttakResultat().getUttakResultatPerioder().isEmpty();
    }

    public static void verifiserAlleAndelerIBeregningErIUttak(BeregningsresultatRegelmodell input) {
        if (brukerHarUttak(input)) {
            var bGAndeler = hentBGAndeler(input);
            var uttakAndeler = hentUttakAndeler(input);
            bGAndeler.forEach(bgAndel -> verifiserAtAndelerMatcher(bgAndel, uttakAndeler));
        }
    }

    private static LocalDate finnSisteUttaksdato(BeregningsresultatRegelmodell input) {
        return input.getUttakResultat()
            .getUttakResultatPerioder()
            .stream()
            .max(Comparator.comparing(UttakResultatPeriode::getTom))
            .map(UttakResultatPeriode::getTom)
            .orElseThrow();
    }

    private static List<UttakAktivitet> hentUttakAndeler(BeregningsresultatRegelmodell input) {
        return input.getUttakResultat()
            .getUttakResultatPerioder()
            .stream()
            .map(UttakResultatPeriode::getUttakAktiviteter)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private static List<BeregningsgrunnlagPrStatus> hentBGAndeler(BeregningsresultatRegelmodell input) {
        // Trenger kun å se på perioder som overlapper med beregning
        var sisteUttaksdato = finnSisteUttaksdato(input);
        return input.getBeregningsgrunnlag()
            .getBeregningsgrunnlagPerioder()
            .stream()
            .filter(bgp -> !bgp.getBeregningsgrunnlagPeriode().getFom().isAfter(sisteUttaksdato))
            .map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatus)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private static void verifiserUttak(UttakAktivitet uttakAndel, List<BeregningsgrunnlagPrStatus> bGAndeler) {
        if (uttakAndel.getAktivitetStatus().equals(AktivitetStatus.ATFL)) {
            finnMatchendeBGArbeidsforhold(uttakAndel, bGAndeler);
        } else {
            finnMatchendeBGAndel(uttakAndel, bGAndeler);
        }
    }

    private static void finnMatchendeBGAndel(UttakAktivitet uttakAndel, List<BeregningsgrunnlagPrStatus> bGAndeler) {
        var matchendeBGAndel = bGAndeler.stream()
            .filter(bgAndel -> BeregningsgrunnlagUttakArbeidsforholdMatcher.matcherGenerellAndel(bgAndel, uttakAndel))
            .findFirst();
        if (matchendeBGAndel.isEmpty()) {
            throw ikkeMatchendeBergeningandelException(uttakAndel.toString(), bGAndeler.toString());
        }
    }

    private static void finnMatchendeBGArbeidsforhold(UttakAktivitet uttakAndel,
                                                      List<BeregningsgrunnlagPrStatus> bGAndeler) {
        var bgArbeidsforhold = bGAndeler.stream()
            .filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.ATFL))
            .map(BeregningsgrunnlagPrStatus::getArbeidsforhold)
            .flatMap(Collection::stream)
            .toList();
        var matchetBGArbfor = bgArbeidsforhold.stream()
            .filter(
                a -> BeregningsgrunnlagUttakArbeidsforholdMatcher.matcherArbeidsforhold(uttakAndel.getArbeidsforhold(),
                    a.getArbeidsforhold()))
            .findFirst();
        if (matchetBGArbfor.isEmpty()) {
            throw ikkeMatchendeBergeningandelException(uttakAndel.toString(), bGAndeler.toString());
        }
    }

    private static void verifiserAtAndelerMatcher(BeregningsgrunnlagPrStatus bgAndel,
                                                  List<UttakAktivitet> uttakAndeler) {
        if (bgAndel.getAktivitetStatus().equals(AktivitetStatus.ATFL)) {
            matchArbeidsforhold(bgAndel.getArbeidsforhold(), uttakAndeler);
        } else {
            matchAndel(bgAndel, uttakAndeler);
        }
    }

    private static void matchAndel(BeregningsgrunnlagPrStatus bgAndel, List<UttakAktivitet> uttakAndeler) {
        var matchetUttaksandel = uttakAndeler.stream()
            .filter(
                uttakAndel -> BeregningsgrunnlagUttakArbeidsforholdMatcher.matcherGenerellAndel(bgAndel, uttakAndel))
            .findFirst();
        if (matchetUttaksandel.isEmpty()) {
            throw ikkeMatchendeUttaksandelException(bgAndel.toString(), uttakAndeler.toString());
        }
    }

    private static void matchArbeidsforhold(List<BeregningsgrunnlagPrArbeidsforhold> arbeidsforhold,
                                            List<UttakAktivitet> uttakAndeler) {
        arbeidsforhold.forEach(arbfor -> {
            var mathendeUttaksaktivitet = finnMatchendeUttakArbeidsforhold(arbfor, uttakAndeler);
            if (mathendeUttaksaktivitet.isEmpty()) {
                throw ikkeMatchendeUttaksandelException(arbfor.toString(), uttakAndeler.toString());
            }
        });
    }

    private static Optional<UttakAktivitet> finnMatchendeUttakArbeidsforhold(BeregningsgrunnlagPrArbeidsforhold beregningsgrunnlagArbeidsforhold,
                                                                             List<UttakAktivitet> uttakAndeler) {
        return uttakAndeler.stream()
            .filter(ua -> BeregningsgrunnlagUttakArbeidsforholdMatcher.matcherArbeidsforhold(
                beregningsgrunnlagArbeidsforhold.getArbeidsforhold(), ua.getArbeidsforhold()))
            .findFirst();
    }

    private static TekniskException ikkeMatchendeBergeningandelException(String uttakAndelBeskrivelse,
                                                                         String beregningsgrunnlagandeler) {
        var msg = String.format(
            "Precondition feilet: Finner ikke matchende beregningsgrunnlagandel for uttaksandel %s . "
                + "Listen med beregningsgrunnlagandeler er: %s", uttakAndelBeskrivelse, beregningsgrunnlagandeler);
        return new TekniskException("FP-370742", msg);
    }

    private static TekniskException ikkeMatchendeUttaksandelException(String beregningsgrunnlagandel,
                                                                      String uttaksandeler) {
        var msg = String.format(
            "Precondition feilet: Finner ikke matchende uttaksandel for beregningsgrunnlagsandel %s . "
                + "Listen med uttaksandeler er: %s", beregningsgrunnlagandel, uttaksandeler);
        return new TekniskException("FP-370743", msg);
    }

}
