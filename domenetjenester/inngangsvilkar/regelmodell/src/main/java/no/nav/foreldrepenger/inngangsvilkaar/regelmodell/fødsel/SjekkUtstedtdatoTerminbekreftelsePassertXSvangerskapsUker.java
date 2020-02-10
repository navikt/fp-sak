package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;
import no.nav.fpsak.nare.specification.LeafSpecification;

public class SjekkUtstedtdatoTerminbekreftelsePassertXSvangerskapsUker extends LeafSpecification<FødselsvilkårGrunnlag> {

    static final String ID = SjekkUtstedtdatoTerminbekreftelsePassertXSvangerskapsUker.class.getSimpleName();

    static final RuleReasonRefImpl IKKE_OPPFYLT_GYLDIG_TERMINBEKREFTELSE_DATO = new RuleReasonRefImpl("1019", "Terminbekreftelsens utstedtdato er før 22. svangerskapsuke");

    SjekkUtstedtdatoTerminbekreftelsePassertXSvangerskapsUker() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(FødselsvilkårGrunnlag t) {
        if (t.isErTerminBekreftelseUtstedtEtterXUker()) {
            return ja();
        } else {
            return nei(IKKE_OPPFYLT_GYLDIG_TERMINBEKREFTELSE_DATO);
        }
    }
}
