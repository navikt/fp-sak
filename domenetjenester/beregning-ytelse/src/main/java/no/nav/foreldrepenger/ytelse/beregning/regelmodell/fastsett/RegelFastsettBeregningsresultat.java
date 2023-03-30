package no.nav.foreldrepenger.ytelse.beregning.regelmodell.fastsett;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatGrunnlag;
import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.Specification;

/**
 * Det mangler dokumentasjon
 */

@RuleDocumentation(value = RegelFastsettBeregningsresultat.ID, specificationReference = "https://confluence.adeo.no/pages/viewpage.action?pageId=262414229")
public class RegelFastsettBeregningsresultat implements RuleService<BeregningsresultatGrunnlag> {

    public static final String ID = "FP_BR 20.1";
    public static final String BESKRIVELSE = "RegelFastsettBeregningsresultat";

    @Override
    public Evaluation evaluer(BeregningsresultatGrunnlag input, Object outputContainer) {
        if (outputContainer instanceof Beregningsresultat beregningsresultat) {
            var mellomregning = new BeregningsresultatRegelmodellMellomregning(input, beregningsresultat);
            return getSpecification().evaluate(mellomregning);
        }
        throw new IllegalArgumentException("Invalid output container class " + outputContainer.getClass());

    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<BeregningsresultatRegelmodellMellomregning> getSpecification() {
        var rs = new Ruleset<BeregningsresultatRegelmodellMellomregning>();
        return rs.beregningsRegel(FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder.ID, FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder.BESKRIVELSE,
                new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder(),
                new Beregnet());
    }
}
