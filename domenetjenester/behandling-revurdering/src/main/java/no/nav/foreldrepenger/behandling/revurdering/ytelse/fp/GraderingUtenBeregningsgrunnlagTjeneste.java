package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.domene.prosess.PeriodeMedGradering;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class GraderingUtenBeregningsgrunnlagTjeneste {

    public static List<BeregningsgrunnlagPrStatusOgAndel> finnesAndelerMedGraderingUtenBG(Beregningsgrunnlag beregningsgrunnlag, List<PeriodeMedGradering> perioderMedGradering) {
        List<BeregningsgrunnlagPrStatusOgAndel> andelerMedGraderingUtenBG = new ArrayList<>();
        perioderMedGradering.forEach(gradering -> {
            var korrektBGAndel = finnMachendeAndelIMatchendePeriode(gradering, beregningsgrunnlag);
            if (korrektBGAndel.isPresent() && harIkkeTilkjentBGEtterRedusering(korrektBGAndel.get())) {
                andelerMedGraderingUtenBG.add(korrektBGAndel.get());
            }
        });
        return andelerMedGraderingUtenBG;
    }

    private static boolean harIkkeTilkjentBGEtterRedusering(BeregningsgrunnlagPrStatusOgAndel andel) {
        return andel.getRedusertPrÅr() != null && andel.getRedusertPrÅr().compareTo(BigDecimal.ZERO) <= 0;
    }

    private static Optional<BeregningsgrunnlagPrStatusOgAndel> finnMachendeAndelIMatchendePeriode(PeriodeMedGradering gradering, Beregningsgrunnlag beregningsgrunnlag) {
        var periode = matchendePeriode(gradering, beregningsgrunnlag);
        return periode.flatMap(p -> finnTilsvarendeAndelIPeriode(gradering, p));
    }

    private static Optional<BeregningsgrunnlagPeriode> matchendePeriode(PeriodeMedGradering gradering, Beregningsgrunnlag beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .stream()
            .filter(p -> p.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(gradering.fom(), gradering.tom())))
            .findFirst();
    }

    private static Optional<BeregningsgrunnlagPrStatusOgAndel> finnTilsvarendeAndelIPeriode(PeriodeMedGradering gradering, BeregningsgrunnlagPeriode periode) {
        return periode.getBeregningsgrunnlagPrStatusOgAndelList().stream().filter((andel) -> bgAndelMatcherGraderingAndel(andel, gradering)).findFirst();
    }

    private static boolean bgAndelMatcherGraderingAndel(BeregningsgrunnlagPrStatusOgAndel andel, PeriodeMedGradering periodeMedGradering) {
        var andelAg = andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver).orElse(null);
        var agMatcher = Objects.equals(andelAg, periodeMedGradering.arbeidsgiver());
        return andel.getAktivitetStatus().equals(periodeMedGradering.aktivitetStatus()) && agMatcher;
    }


}
