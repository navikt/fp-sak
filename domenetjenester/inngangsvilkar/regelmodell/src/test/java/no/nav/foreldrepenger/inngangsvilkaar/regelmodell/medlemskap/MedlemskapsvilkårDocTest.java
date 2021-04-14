package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.nare.doc.RuleDescriptionDigraph;

public class MedlemskapsvilkårDocTest {

    @Test
    public void test_documentation() throws Exception {
        var vilkår = new Medlemskapsvilkår().getSpecification();
        var digraph = new RuleDescriptionDigraph(vilkår.ruleDescription());

        @SuppressWarnings("unused") var json = digraph.toJson();

//        System.out.println(json);
    }
}
