package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelUtfallMerknad;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

public class SjekkSøktOmTermin extends LeafSpecification<FødselsvilkårGrunnlag> {

    private static final String ID = SjekkSøktOmTermin.class.getSimpleName();

    static final MerknadRuleReasonRef IKKE_OPPFYLT_BARN_DOKUMENTERT_PÅ_FAR_MEDMOR =
        new MerknadRuleReasonRef(RegelUtfallMerknad.RVM_1027, "Søker er ikke dokumentert som barnets far/medmor");

    SjekkSøktOmTermin() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(FødselsvilkårGrunnlag t) {
        if (t.erSøktOmTermin()) {
            return ja();
        }
        return nei();
    }
}
