package no.nav.foreldrepenger.ytelse.beregning.regler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodellMellomregning;
import no.nav.fpsak.nare.doc.RuleDescriptionDigraph;
import no.nav.fpsak.nare.specification.Specification;

public class RegelFastsettBeregningsresultatDocTest {

    @Test
    public void test_documentation() {
        Specification<BeregningsresultatRegelmodellMellomregning> beregning = new RegelFastsettBeregningsresultat().getSpecification();
        RuleDescriptionDigraph digraph = new RuleDescriptionDigraph(beregning.ruleDescription());

        String json = digraph.toJson();

        assertThat(json.indexOf("\"edges\" : [ ]")).isLessThan(0);
    }
}
