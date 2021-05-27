package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkBrukerErAvklartSomEUEØSStatsborger.ID)
public class SjekkBrukerErAvklartSomEUEØSStatsborger extends LeafSpecification<MedlemskapsvilkårGrunnlag> {

    static final String ID = "FP_VK_2.12";

    SjekkBrukerErAvklartSomEUEØSStatsborger() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedlemskapsvilkårGrunnlag grunnlag) {
        if (grunnlag.brukerBorgerAvEUEOS()) {
            return ja();
        }
        return nei();
    }

}
