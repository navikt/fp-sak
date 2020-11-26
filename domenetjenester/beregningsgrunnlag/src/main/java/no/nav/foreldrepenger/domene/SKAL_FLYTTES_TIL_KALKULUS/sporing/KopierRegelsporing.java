package no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.sporing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriodeRegelSporing;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriodeRegelType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRegelSporing;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRegelType;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

public class KopierRegelsporing {

    private KopierRegelsporing() {
        // Skjul
    }

    /**
     * Kopierer regelsporinger fra det aktive beregningsgrunnlaget til det nye beregningsgrunnlaget (som skal lagres ned)
     * <p>
     * Kalkulus returnerer kun nye regelsporinger pr steg, og gamle/eksisterende regelsporinger må derfor kopieres over for å bli med til det nye grunnlaget.
     * @param nyttGrunnlag      Nytt beregningsgrunnlag fra kalkulus
     * @param tidligereAggregat Gjeldende aktive beregningsgrunnlag
     */
    // TODO (PEKERN) Sjå på mulighet for å fjerne side-effects
    public static void kopierRegelsporingerTilGrunnlag(BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> tidligereAggregat) {
        var grunnlagBuilder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(nyttGrunnlag);
        if (nyttGrunnlag.getBeregningsgrunnlag().isPresent() && tidligereAggregat.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag).isPresent()) {
            BeregningsgrunnlagEntitet.Builder bgBuilder = grunnlagBuilder.getBeregningsgrunnlagBuilder();
            var sporingerSomKopieres = finnRegelsporingerSomSkalKopieres(nyttGrunnlag.getBeregningsgrunnlag().get(), tidligereAggregat.get().getBeregningsgrunnlag().orElseThrow());
            sporingerSomKopieres.forEach(rs -> bgBuilder.medRegelSporing(rs.getRegelInput(), rs.getRegelEvaluering(), rs.getRegelType()));
            var sporingerPeriodeSomKopieres = finnRegelsporingerSomSkalKopieresFraPeriode(nyttGrunnlag.getBeregningsgrunnlag().get(), tidligereAggregat.get().getBeregningsgrunnlag().orElseThrow());
            sporingerPeriodeSomKopieres.forEach((periode, regelsporing) -> {
                var periodeBuilders = bgBuilder.getPeriodeBuilders(periode);
                periodeBuilders.forEach(b -> regelsporing.forEach(sp -> b.medRegelEvaluering(sp.getRegelInput(), sp.getRegelEvaluering(), sp.getRegelType())));
            });
        }
    }

    public static List<BeregningsgrunnlagRegelSporing> finnRegelsporingerSomSkalKopieres(BeregningsgrunnlagEntitet bg, BeregningsgrunnlagEntitet tidligereBg) {
        Map<BeregningsgrunnlagRegelType, BeregningsgrunnlagRegelSporing> tidligereSporinger = finnGrunnlagsporinger(tidligereBg);
        Map<BeregningsgrunnlagRegelType, BeregningsgrunnlagRegelSporing> grunnlagSporinger = finnGrunnlagsporinger(bg);
        return tidligereSporinger.entrySet().stream().filter(e -> !grunnlagSporinger.containsKey(e.getKey()))
            .map(Map.Entry::getValue)
            .map(BeregningsgrunnlagRegelSporing::new)
            .collect(Collectors.toList());
    }

    public static Map<ÅpenDatoIntervallEntitet, List<BeregningsgrunnlagPeriodeRegelSporing>> finnRegelsporingerSomSkalKopieresFraPeriode(BeregningsgrunnlagEntitet bg, BeregningsgrunnlagEntitet tidligereBg) {
        List<BeregningsgrunnlagPeriode> tidligereBgPerioder = tidligereBg.getBeregningsgrunnlagPerioder();
        Map<ÅpenDatoIntervallEntitet, List<BeregningsgrunnlagPeriodeRegelSporing>> regelsporinger = new HashMap<>();
        bg.getBeregningsgrunnlagPerioder().forEach(periode -> {
            BeregningsgrunnlagPeriode tidligerePeriode = finnTidligerePeriode(tidligereBgPerioder, periode);
            Map<BeregningsgrunnlagPeriodeRegelType, BeregningsgrunnlagPeriodeRegelSporing> regelSporinger = periode.getRegelSporinger();
            Map<BeregningsgrunnlagPeriodeRegelType, BeregningsgrunnlagPeriodeRegelSporing> tidligerePeriodeSporinger = tidligerePeriode.getRegelSporinger();
            var sporinger = tidligerePeriodeSporinger.entrySet().stream().filter((e) -> !regelSporinger.containsKey(e.getKey()))
                .map(Map.Entry::getValue)
                .map(BeregningsgrunnlagPeriodeRegelSporing::new)
                .collect(Collectors.toList());
            regelsporinger.put(periode.getPeriode(), sporinger);
        });
        return regelsporinger;
    }

    private static BeregningsgrunnlagPeriode finnTidligerePeriode(List<BeregningsgrunnlagPeriode> tidligereBgPerioder, BeregningsgrunnlagPeriode periode) {
        return tidligereBgPerioder.stream().filter(p -> p.getPeriode()
            .overlapper(periode.getPeriode())).findFirst().orElseThrow(() -> new IllegalStateException("Forventer overlappende periode i tidligere grunnlag"));
    }

    private static Map<BeregningsgrunnlagRegelType, BeregningsgrunnlagRegelSporing> finnGrunnlagsporinger(BeregningsgrunnlagEntitet beregningsgrunnlagEntitet) {
        return beregningsgrunnlagEntitet.getRegelSporinger();
    }

}
