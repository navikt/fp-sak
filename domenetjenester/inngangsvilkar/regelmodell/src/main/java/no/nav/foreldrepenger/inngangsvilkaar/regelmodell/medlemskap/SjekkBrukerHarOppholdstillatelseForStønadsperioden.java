package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkBrukerHarOppholdstillatelseForStønadsperioden.ID)
public class SjekkBrukerHarOppholdstillatelseForStønadsperioden extends LeafSpecification<MedlemskapsvilkårGrunnlag> {

    static final String ID = "FP_VK_2.10";

    SjekkBrukerHarOppholdstillatelseForStønadsperioden() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedlemskapsvilkårGrunnlag grunnlag) {
        if (grunnlag.brukerHarOppholdstillatelse()) {
            return ja();
        }
        return nei();
    }

}
