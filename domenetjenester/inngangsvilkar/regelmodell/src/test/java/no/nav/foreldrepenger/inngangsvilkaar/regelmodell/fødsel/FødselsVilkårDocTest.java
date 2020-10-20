package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.nare.doc.RuleDescriptionDigraph;
import no.nav.fpsak.nare.specification.Specification;

public class FødselsVilkårDocTest {

    @Test
    public void test_documentation() {
        Specification<FødselsvilkårGrunnlag> vilkår = new FødselsvilkårMor().getSpecification();
        RuleDescriptionDigraph digraph = new RuleDescriptionDigraph(vilkår.ruleDescription());

        @SuppressWarnings("unused")
        String json = digraph.toJson();

//        System.out.println(json);
    }
}
