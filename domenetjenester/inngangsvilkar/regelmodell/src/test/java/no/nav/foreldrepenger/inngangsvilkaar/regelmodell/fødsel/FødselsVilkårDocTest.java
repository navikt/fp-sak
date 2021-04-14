package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.nare.doc.RuleDescriptionDigraph;

public class FødselsVilkårDocTest {

    @Test
    public void test_documentation() {
        var vilkår = new FødselsvilkårMor().getSpecification();
        var digraph = new RuleDescriptionDigraph(vilkår.ruleDescription());

        @SuppressWarnings("unused") var json = digraph.toJson();

//        System.out.println(json);
    }
}
