package no.nav.foreldrepenger.inngangsvilkaar.søknad;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.søknadsfrist.Søknadsfristvilkår;
import no.nav.fpsak.nare.doc.RuleDescriptionDigraph;

public class SøknadsfristVilkårDocTest {

    @Test
    public void test_documentation() throws Exception {
        var vilkår = new Søknadsfristvilkår().getSpecification();
        var digraph = new RuleDescriptionDigraph(vilkår.ruleDescription());

        @SuppressWarnings("unused") var json = digraph.toJson();

//        System.out.println(json);
    }
}
