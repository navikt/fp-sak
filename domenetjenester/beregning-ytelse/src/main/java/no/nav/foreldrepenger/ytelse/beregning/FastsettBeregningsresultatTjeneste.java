package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFraRegelTilVL;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.foreldrepenger.ytelse.beregning.regler.RegelFastsettBeregningsresultat;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

public final class FastsettBeregningsresultatTjeneste {

    public static BeregningsresultatEntitet fastsettBeregningsresultat(BeregningsresultatRegelmodell regelmodell) {
        // Kalle regel
        var regel = new RegelFastsettBeregningsresultat();
        var outputContainer = Beregningsresultat.builder().build();
        var evaluation = regel.evaluer(regelmodell, outputContainer);
        var sporing = EvaluationSerializer.asJson(evaluation);

        // Map tilbake til domenemodell fra regelmodell
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput(toJson(regelmodell))
            .medRegelSporing(sporing)
            .build();

        MapBeregningsresultatFraRegelTilVL.mapFra(outputContainer, beregningsresultat);

        return beregningsresultat;
    }

    private static String toJson(BeregningsresultatRegelmodell grunnlag) {
        return DefaultJsonMapper.toPrettyJson(grunnlag);
    }
}
