package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

public class SjekkFødselErRegistrert extends LeafSpecification<FødselsvilkårGrunnlag> {

    static final String ID = SjekkFødselErRegistrert.class.getSimpleName();

    SjekkFødselErRegistrert() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(FødselsvilkårGrunnlag t) {
        var fødselRegistrert = t.bekreftetFødselsdato() != null && t.antallBarn() > 0;
        if (fødselRegistrert) {
            return ja();
        }
        return nei();
    }

}
