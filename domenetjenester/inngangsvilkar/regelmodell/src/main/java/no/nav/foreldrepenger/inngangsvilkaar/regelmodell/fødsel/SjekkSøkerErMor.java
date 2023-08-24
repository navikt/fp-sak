package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelUtfallMerknad;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

public class SjekkSøkerErMor extends LeafSpecification<FødselsvilkårGrunnlag> {

    static final String ID = SjekkSøkerErMor.class.getSimpleName();

    static final MerknadRuleReasonRef IKKE_OPPFYLT_FØDSEL_REGISTRERT_SØKER_IKKE_BARNETS_MOR =
        new MerknadRuleReasonRef(RegelUtfallMerknad.RVM_1002, "Søker er ikke barnets mor");

    SjekkSøkerErMor() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(FødselsvilkårGrunnlag t) {
        var erBarnetsMor = RegelSøkerRolle.MORA.equals(t.søkerRolle());
        if (erBarnetsMor) {
            return ja();
        }
        return nei();
    }

    @Override
    public String beskrivelse() {
        return "Sjekk søker er mor.";
    }
}
