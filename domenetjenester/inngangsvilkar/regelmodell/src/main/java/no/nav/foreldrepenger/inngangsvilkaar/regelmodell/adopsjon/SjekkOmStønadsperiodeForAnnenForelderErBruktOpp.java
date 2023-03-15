package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelUtfallMerknad;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

class SjekkOmStønadsperiodeForAnnenForelderErBruktOpp extends LeafSpecification<AdopsjonsvilkårGrunnlag> {

    static final String ID = "FP_VK_16";

    static final MerknadRuleReasonRef STEBARNSADOPSJON_IKKE_FLERE_DAGER_IGJEN =
        new MerknadRuleReasonRef(RegelUtfallMerknad.RVM_1051, "Stebarnsadopsjon ikke flere dager igjen");


    SjekkOmStønadsperiodeForAnnenForelderErBruktOpp() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(AdopsjonsvilkårGrunnlag grunnlag) {
        if (grunnlag.erStønadsperiodeBruktOpp()) {
            return ja();
        }
        // hvis stønadsperioden ikke er brukt opp = det er flere dager igjen
        return nei();
    }
}
