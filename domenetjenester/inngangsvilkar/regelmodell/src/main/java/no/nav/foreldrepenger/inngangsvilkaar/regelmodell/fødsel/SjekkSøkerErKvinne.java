package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelKjønn;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;
import no.nav.fpsak.nare.specification.LeafSpecification;

public class SjekkSøkerErKvinne extends LeafSpecification<FødselsvilkårGrunnlag> {

    static final String ID = SjekkSøkerErKvinne.class.getSimpleName();

    static final RuleReasonRefImpl IKKE_OPPFYLT_SØKER_ER_KVINNE = new RuleReasonRefImpl("1003", "Søker er ikke KVINNE({0}), er {1}");

    SjekkSøkerErKvinne() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(FødselsvilkårGrunnlag t) {
        var erKvinne = RegelKjønn.KVINNE.equals(t.søkersKjønn());
        if (erKvinne) {
            return ja();
        }
        return nei(IKKE_OPPFYLT_SØKER_ER_KVINNE, RegelKjønn.KVINNE, t.søkersKjønn());
    }

}
