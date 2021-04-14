package no.nav.foreldrepenger.ytelse.beregning.regler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.nare.doc.RuleDescriptionDigraph;

public class RegelFastsettBeregningsresultatDocTest {

    @Test
    public void test_documentation() {
        var beregning = new RegelFastsettBeregningsresultat().getSpecification();
        var digraph = new RuleDescriptionDigraph(beregning.ruleDescription());

        var json = digraph.toJson();

        assertThat(json.indexOf("\"edges\" : [ ]")).isLessThan(0);
    }
}
