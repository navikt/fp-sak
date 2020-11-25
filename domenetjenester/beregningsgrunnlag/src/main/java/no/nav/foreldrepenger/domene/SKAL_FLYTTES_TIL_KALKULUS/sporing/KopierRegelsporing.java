package no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.sporing;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriodeRegelSporing;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriodeRegelType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRegelSporing;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRegelType;

public class KopierRegelsporing {

    private KopierRegelsporing() {
        // Skjul
    }

    /** Kopierer regelsporinger fra det aktive beregningsgrunnlaget til det nye beregningsgrunnlaget (som skal lagres ned)
     *
     * Kalkulus returnerer kun nye regelsporinger pr steg, og gamle/eksisterende regelsporinger må derfor kopieres over for å bli med til det nye grunnlaget.
     *
     * @param tidligereAggregat Gjeldende aktive beregningsgrunnlag
     * @param nyttGrunnlag Nytt beregningsgrunnlag fra kalkulus
     */
    //  TODO (PEKERN) Fjern side-effects
    public static void kopierRegelsporinger(Optional<BeregningsgrunnlagGrunnlagEntitet> tidligereAggregat, BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag) {
        if (nyttGrunnlag.getBeregningsgrunnlag().isPresent() && tidligereAggregat.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag).isPresent()) {
            kopierRegelsporingerForGrunnlag(nyttGrunnlag.getBeregningsgrunnlag().get(), tidligereAggregat.get().getBeregningsgrunnlag().orElseThrow());
            kopierRegelSporingerForPerioder(nyttGrunnlag.getBeregningsgrunnlag().get(), tidligereAggregat.get().getBeregningsgrunnlag().orElseThrow());
        }
    }

    private static void kopierRegelsporingerForGrunnlag(BeregningsgrunnlagEntitet bg, BeregningsgrunnlagEntitet tidligereBg) {
        Map<BeregningsgrunnlagRegelType, BeregningsgrunnlagRegelSporing> tidligereSporinger = finnGrunnlagsporinger(tidligereBg);
        Map<BeregningsgrunnlagRegelType, BeregningsgrunnlagRegelSporing> grunnlagSporinger = finnGrunnlagsporinger(bg);
        BeregningsgrunnlagEntitet.Builder bgBuilder = BeregningsgrunnlagEntitet.builder(bg);
        tidligereSporinger.forEach((k, v) -> {
            if (!grunnlagSporinger.containsKey(k)) {
                bgBuilder.medRegelSporing(v.getRegelInput(), v.getRegelEvaluering(), k);
            }
        });
    }

    private static void kopierRegelSporingerForPerioder(BeregningsgrunnlagEntitet bg, BeregningsgrunnlagEntitet tidligereBg) {
        List<BeregningsgrunnlagPeriode> tidligereBgPerioder = tidligereBg.getBeregningsgrunnlagPerioder();
        bg.getBeregningsgrunnlagPerioder().forEach(periode -> {
            BeregningsgrunnlagPeriode.Builder periodeBuilder = BeregningsgrunnlagPeriode.builder(periode);
            BeregningsgrunnlagPeriode tidligerePeriode = finnTidligerePeriode(tidligereBgPerioder, periode);
            Map<BeregningsgrunnlagPeriodeRegelType, BeregningsgrunnlagPeriodeRegelSporing> regelSporinger = periode.getRegelSporinger();
            Map<BeregningsgrunnlagPeriodeRegelType, BeregningsgrunnlagPeriodeRegelSporing> tidligerePeriodeSporinger = tidligerePeriode.getRegelSporinger();
            tidligerePeriodeSporinger.forEach((type, sporing) -> {
                if (!regelSporinger.containsKey(type)) {
                    periodeBuilder.medRegelEvaluering(sporing.getRegelInput(), sporing.getRegelEvaluering(), sporing.getRegelType());
                }
            });
        });
    }

    private static BeregningsgrunnlagPeriode finnTidligerePeriode(List<BeregningsgrunnlagPeriode> tidligereBgPerioder, BeregningsgrunnlagPeriode periode) {
        return tidligereBgPerioder.stream().filter(p -> p.getPeriode()
            .overlapper(periode.getPeriode())).findFirst().orElseThrow(() -> new IllegalStateException("Forventer overlappende periode i tidligere grunnlag"));
    }

    private static Map<BeregningsgrunnlagRegelType, BeregningsgrunnlagRegelSporing> finnGrunnlagsporinger(BeregningsgrunnlagEntitet beregningsgrunnlagEntitet) {
        return beregningsgrunnlagEntitet.getRegelSporinger();
    }

}
