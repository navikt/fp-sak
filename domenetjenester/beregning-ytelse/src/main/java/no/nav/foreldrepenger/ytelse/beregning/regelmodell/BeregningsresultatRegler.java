package no.nav.foreldrepenger.ytelse.beregning.regelmodell;

import java.util.stream.Collectors;

import no.nav.foreldrepenger.stønadskonto.regelmodell.KontoRegelFeil;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.fastsett.RegelFastsettBeregningsresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.RegelBeregnFeriepenger;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;
import no.nav.fpsak.nare.json.JsonOutput;
import no.nav.fpsak.nare.json.NareJsonException;

public final class BeregningsresultatRegler {

    private BeregningsresultatRegler() {
    }

    public static FastsattBeregningsresultat fastsettBeregningsresultat(BeregningsresultatGrunnlag grunnlag) {
        // Kalle regel
        var outputContainer = Beregningsresultat.opprett();
        var evaluation = new RegelFastsettBeregningsresultat().evaluer(grunnlag, outputContainer);
        var sporing = EvaluationSerializer.asJson(evaluation);

        // Map tilbake til domenemodell fra regelmodell
        return new FastsattBeregningsresultat(outputContainer, grunnlag, toJson(grunnlag), sporing, null); // TODO versjon når eget repo
    }

    public static FastsattFeriepengeresultat fastsettFeriepenger(BeregningsresultatFeriepengerGrunnlag grunnlag) {

        var regelInput = toJson(grunnlag);
        var mellomregningsperioder = grunnlag.getBeregningsresultatPerioder().stream()
            .map(BeregningsresultatPeriode::copyUtenFeriepenger)
            .collect(Collectors.toList());
        var mellomregning = new BeregningsresultatFeriepengerRegelModell(grunnlag, mellomregningsperioder);

        var evaluation = new RegelBeregnFeriepenger().evaluer(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);

        var resultat = new BeregningsresultatFeriepengerResultat(mellomregning.getBeregningsresultatPerioder(), mellomregning.getFeriepengerPeriode());

        return new FastsattFeriepengeresultat(resultat, grunnlag, regelInput, sporing, null); // TODO versjon når eget repo
    }

    private static String toJson(Object grunnlag) {
        try {
            return JsonOutput.asJson(grunnlag);
        } catch (NareJsonException e) {
            throw new KontoRegelFeil("Kunne ikke serialisere regelinput for beregning av tilkjent/feriepenger.", e);
        }
    }
}
