package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.søknadsfrist;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(value="FP_VK_3.1")
public class SjekkElektroniskSøknad extends LeafSpecification<SøknadsfristvilkårGrunnlag> {
    static final String ID = SjekkElektroniskSøknad.class.getSimpleName();

    SjekkElektroniskSøknad() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(SøknadsfristvilkårGrunnlag t) {
        var erElektroniskSøknad = t.elektroniskSoeknad();  // ellers papir
        if(erElektroniskSøknad) {
            return ja();
        }
        return nei();
    }

}
