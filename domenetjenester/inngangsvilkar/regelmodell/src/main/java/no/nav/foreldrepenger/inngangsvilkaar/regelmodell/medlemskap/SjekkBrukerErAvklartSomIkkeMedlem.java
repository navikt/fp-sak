package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelUtfallMerknad;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkBrukerErAvklartSomIkkeMedlem.ID)
public class SjekkBrukerErAvklartSomIkkeMedlem extends LeafSpecification<MedlemskapsvilkårGrunnlag> {

    static final String ID = "FP_VK_2.13";

    static final MerknadRuleReasonRef IKKE_OPPFYLT_BRUKER_ER_OPPFØRT_SOM_IKKE_MEDLEM =
        new MerknadRuleReasonRef(RegelUtfallMerknad.RVM_1020, "Bruker er registrert som ikke medlem.");

    SjekkBrukerErAvklartSomIkkeMedlem() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedlemskapsvilkårGrunnlag grunnlag) {
        if (!grunnlag.brukerErMedlem()) {
            return ja();
        }
        return nei();
    }
}
