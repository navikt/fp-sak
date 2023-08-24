package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelUtfallMerknad;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkBrukerErAvklartMedLovligOpphold.ID)
public class SjekkBrukerErAvklartMedLovligOpphold extends LeafSpecification<MedlemskapsvilkårGrunnlag> {

    static final String ID = "FP_VK_2.12.1";

    static final MerknadRuleReasonRef IKKE_OPPFYLT_BRUKER_HAR_IKKE_LOVLIG_OPPHOLD =
        new MerknadRuleReasonRef(RegelUtfallMerknad.RVM_1023, "Bruker har ikke lovlig opphold.");

    SjekkBrukerErAvklartMedLovligOpphold() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedlemskapsvilkårGrunnlag grunnlag) {
        if (grunnlag.brukerAvklartLovligOppholdINorge()) {
            return ja();
        }
        return nei(IKKE_OPPFYLT_BRUKER_HAR_IKKE_LOVLIG_OPPHOLD);
    }
}
