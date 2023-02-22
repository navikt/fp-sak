package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkBrukerErAvklartSomIkkeBosatt.ID)
public class SjekkBrukerErAvklartSomIkkeBosatt extends LeafSpecification<MedlemskapsvilkårGrunnlag> {

    static final String ID = "FP_VK_2.x";  //TODO FL Hva skal stå her?

    SjekkBrukerErAvklartSomIkkeBosatt() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedlemskapsvilkårGrunnlag grunnlag) {
        if (!grunnlag.brukerAvklartBosatt()) {
            return ja();
        }
        return nei();
    }
}
