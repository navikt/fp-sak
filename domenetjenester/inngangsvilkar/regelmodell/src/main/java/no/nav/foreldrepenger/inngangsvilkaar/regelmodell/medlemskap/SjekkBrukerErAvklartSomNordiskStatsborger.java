package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkBrukerErAvklartSomNordiskStatsborger.ID)
public class SjekkBrukerErAvklartSomNordiskStatsborger extends LeafSpecification<MedlemskapsvilkårGrunnlag> {

    static final String ID = "FP_VK_2.11";

    SjekkBrukerErAvklartSomNordiskStatsborger() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedlemskapsvilkårGrunnlag grunnlag) {
        if (grunnlag.brukerNorskNordisk()) {
            return ja();
        }
        return nei();
    }

}
