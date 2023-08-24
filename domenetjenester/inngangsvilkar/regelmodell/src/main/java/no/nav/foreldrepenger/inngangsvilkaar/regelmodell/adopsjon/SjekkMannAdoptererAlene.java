package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelUtfallMerknad;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkMannAdoptererAlene.ID)
class SjekkMannAdoptererAlene extends LeafSpecification<AdopsjonsvilkårGrunnlag> {

    static final String ID = "FP_VK_4.2";

    static final MerknadRuleReasonRef IKKE_OPPFYLT_MANN_ADOPTERER_IKKE_ALENE =
        new MerknadRuleReasonRef(RegelUtfallMerknad.RVM_1006, "Mann adopterer ikke alene.");

    SjekkMannAdoptererAlene() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(AdopsjonsvilkårGrunnlag grunnlag) {
        if (grunnlag.mannAdoptererAlene()) {
            return ja();
        }
        return nei(IKKE_OPPFYLT_MANN_ADOPTERER_IKKE_ALENE);
    }
}
