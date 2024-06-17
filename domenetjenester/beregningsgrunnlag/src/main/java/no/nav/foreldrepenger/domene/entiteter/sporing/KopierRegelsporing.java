package no.nav.foreldrepenger.domene.entiteter.sporing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriodeRegelSporing;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRegelSporing;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

public class KopierRegelsporing {

    private KopierRegelsporing() {
        // Skjul
    }

    /**
     * Kopierer regelsporinger fra det aktive beregningsgrunnlaget til det nye beregningsgrunnlaget (som skal lagres ned)
     * <p>
     * Kalkulus returnerer kun nye regelsporinger pr steg, og gamle/eksisterende regelsporinger må derfor kopieres over for å bli med til det nye grunnlaget.
     *
     * @param nyttGrunnlag      Nytt beregningsgrunnlag fra kalkulus
     * @param tidligereAggregat Gjeldende aktive beregningsgrunnlag
     */
    // TODO (PEKERN) Sjå på mulighet for å fjerne side-effects
    public static void kopierRegelsporingerTilGrunnlag(BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag,
                                                       Optional<BeregningsgrunnlagGrunnlagEntitet> tidligereAggregat) {
        var grunnlagBuilder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(nyttGrunnlag);
        var nyttBG = nyttGrunnlag.getBeregningsgrunnlag();
        if (nyttBG.isPresent() && tidligereAggregat.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag).isPresent()) {
            var bgBuilder = grunnlagBuilder.getBeregningsgrunnlagBuilder();
            var sporingerSomKopieres = finnRegelsporingerSomSkalKopieres(nyttBG.get(),
                tidligereAggregat.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag).orElseThrow());
            sporingerSomKopieres.forEach(rs -> bgBuilder.medRegelSporing(rs.getRegelInput(), rs.getRegelEvaluering(), rs.getRegelType()));
            var sporingerPeriodeSomKopieres = finnRegelsporingerSomSkalKopieresFraPeriode(nyttBG.get(),
                tidligereAggregat.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag).orElseThrow());
            sporingerPeriodeSomKopieres.forEach((periode, regelsporing) -> {
                var periodeBuilders = bgBuilder.getPeriodeBuilders(periode);
                periodeBuilders.forEach(
                    b -> regelsporing.forEach(sp -> b.medRegelEvaluering(sp.getRegelInput(), sp.getRegelEvaluering(), sp.getRegelType())));
            });
        }
    }

    public static List<BeregningsgrunnlagRegelSporing> finnRegelsporingerSomSkalKopieres(BeregningsgrunnlagEntitet bg,
                                                                                         BeregningsgrunnlagEntitet tidligereBg) {
        var tidligereSporinger = finnGrunnlagsporinger(tidligereBg);
        var grunnlagSporinger = finnGrunnlagsporinger(bg);
        return tidligereSporinger.entrySet()
            .stream()
            .filter(e -> !grunnlagSporinger.containsKey(e.getKey()))
            .map(Map.Entry::getValue)
            .map(BeregningsgrunnlagRegelSporing::new)
            .toList();
    }

    public static Map<ÅpenDatoIntervallEntitet, List<BeregningsgrunnlagPeriodeRegelSporing>> finnRegelsporingerSomSkalKopieresFraPeriode(
        BeregningsgrunnlagEntitet bg,
        BeregningsgrunnlagEntitet tidligereBg) {
        var tidligereBgPerioder = tidligereBg.getBeregningsgrunnlagPerioder();
        Map<ÅpenDatoIntervallEntitet, List<BeregningsgrunnlagPeriodeRegelSporing>> regelsporinger = new HashMap<>();
        bg.getBeregningsgrunnlagPerioder().forEach(periode -> {
            var tidligerePeriode = finnTidligerePeriode(tidligereBgPerioder, periode);
            var regelSporinger = periode.getRegelSporinger();
            var tidligerePeriodeSporinger = tidligerePeriode.getRegelSporinger();
            var sporinger = tidligerePeriodeSporinger.entrySet()
                .stream()
                .filter(e -> !regelSporinger.containsKey(e.getKey()))
                .map(Map.Entry::getValue)
                .map(BeregningsgrunnlagPeriodeRegelSporing::new)
                .toList();
            regelsporinger.put(periode.getPeriode(), sporinger);
        });
        return regelsporinger;
    }

    private static BeregningsgrunnlagPeriode finnTidligerePeriode(List<BeregningsgrunnlagPeriode> tidligereBgPerioder,
                                                                  BeregningsgrunnlagPeriode periode) {
        return tidligereBgPerioder.stream()
            .filter(p -> p.getPeriode().overlapper(periode.getPeriode()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Forventer overlappende periode i tidligere grunnlag"));
    }

    private static Map<BeregningsgrunnlagRegelType, BeregningsgrunnlagRegelSporing> finnGrunnlagsporinger(BeregningsgrunnlagEntitet beregningsgrunnlagEntitet) {
        return beregningsgrunnlagEntitet.getRegelSporinger();
    }

}
