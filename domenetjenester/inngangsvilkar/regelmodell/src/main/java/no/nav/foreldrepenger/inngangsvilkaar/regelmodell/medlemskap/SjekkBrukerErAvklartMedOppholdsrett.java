package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelUtfallMerknad;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkBrukerErAvklartMedOppholdsrett.ID)
public class SjekkBrukerErAvklartMedOppholdsrett extends LeafSpecification<MedlemskapsvilkårGrunnlag> {

    static final String ID = "FP_VK_2.12.2"; //TODO: skal det være samme ID som lovlig opphold???

    static final MerknadRuleReasonRef IKKE_OPPFYLT_BRUKER_HAR_IKKE_OPPHOLDSRETT =
        new MerknadRuleReasonRef(RegelUtfallMerknad.RVM_1024, "Bruker har ikke oppholdsrett.");

    SjekkBrukerErAvklartMedOppholdsrett() {
        super(ID);
    }


    @Override
    public Evaluation evaluate(MedlemskapsvilkårGrunnlag grunnlag) {
        if (grunnlag.brukerAvklartOppholdsrett()) {
            return ja();
        }
        return nei(IKKE_OPPFYLT_BRUKER_HAR_IKKE_OPPHOLDSRETT);
    }
}
