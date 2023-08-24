package no.nav.foreldrepenger.domene.rest.dto;

import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

/**
 * Tjeneste som finner andeler basert på informasjon om andelen (arbeidsforholdId, andelsnr)
 */
public class MatchBeregningsgrunnlagTjeneste {

    private MatchBeregningsgrunnlagTjeneste() {
        // Skjul
    }

    public static Optional<BeregningsgrunnlagPeriode> finnOverlappendePeriodeOmKunEnFinnes(BeregningsgrunnlagPeriode periode,
                                                                                           Optional<BeregningsgrunnlagEntitet> forrigeGrunnlag) {
        var matchedePerioder = forrigeGrunnlag.map(bg ->
            bg.getBeregningsgrunnlagPerioder().stream()
            .filter(periodeIGjeldendeGrunnlag -> periode.getPeriode()
                .overlapper(periodeIGjeldendeGrunnlag.getPeriode())).toList()).orElse(Collections.emptyList());
        if (matchedePerioder.size() == 1) {
            return Optional.of(matchedePerioder.get(0));
        }
        return Optional.empty();
    }


    public static BeregningsgrunnlagPeriode finnPeriodeIBeregningsgrunnlag(BeregningsgrunnlagPeriode periode, BeregningsgrunnlagEntitet gjeldendeBeregningsgrunnlag) {

        if (periode.getBeregningsgrunnlagPeriodeFom().isBefore(gjeldendeBeregningsgrunnlag.getSkjæringstidspunkt())) {
            return gjeldendeBeregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
                .min(Comparator.comparing(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom))
                .orElseThrow(() -> new IllegalStateException("Fant ingen perioder i beregningsgrunnlag."));
        }

        return gjeldendeBeregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .filter(bgPeriode -> inkludererBeregningsgrunnlagPeriodeDato(bgPeriode, periode.getBeregningsgrunnlagPeriodeFom()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Finner ingen korresponderende periode i det fastsatte grunnlaget"));
    }

    private static boolean inkludererBeregningsgrunnlagPeriodeDato(BeregningsgrunnlagPeriode periode, LocalDate dato) {
        return !periode.getBeregningsgrunnlagPeriodeFom().isAfter(dato) && (periode.getBeregningsgrunnlagPeriodeTom() == null || !periode.getBeregningsgrunnlagPeriodeTom().isBefore(dato));
    }






}
