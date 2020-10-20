package no.nav.foreldrepenger.inngangsvilkaar.søknad;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.søknadsfrist.SoeknadsfristvilkarGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.søknadsfrist.Søknadsfristvilkår;
import no.nav.fpsak.nare.doc.RuleDescriptionDigraph;
import no.nav.fpsak.nare.specification.Specification;

public class SøknadsfristVilkårDocTest {

    @Test
    public void test_documentation() throws Exception {
        Specification<SoeknadsfristvilkarGrunnlag> vilkår = new Søknadsfristvilkår().getSpecification();
        RuleDescriptionDigraph digraph = new RuleDescriptionDigraph(vilkår.ruleDescription());

        @SuppressWarnings("unused")
        String json = digraph.toJson();

//        System.out.println(json);
    }
}
