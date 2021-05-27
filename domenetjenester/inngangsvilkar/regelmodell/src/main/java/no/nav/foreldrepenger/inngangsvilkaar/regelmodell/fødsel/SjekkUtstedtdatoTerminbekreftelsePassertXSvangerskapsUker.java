package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;
import no.nav.fpsak.nare.specification.LeafSpecification;

public class SjekkUtstedtdatoTerminbekreftelsePassertXSvangerskapsUker extends LeafSpecification<FødselsvilkårGrunnlag> {

    static final String ID = SjekkUtstedtdatoTerminbekreftelsePassertXSvangerskapsUker.class.getSimpleName();

    static final RuleReasonRefImpl IKKE_OPPFYLT_GYLDIG_TERMINBEKREFTELSE_DATO = new RuleReasonRefImpl("1019",
        "Terminbekreftelse utstedt {0} er før svangerskapsuke 22 (termindato ({1}))");

    SjekkUtstedtdatoTerminbekreftelsePassertXSvangerskapsUker() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(FødselsvilkårGrunnlag t) {
        if (t.erSøktOmTermin() && t.terminbekreftelseTermindato() == null) {
            throw new IllegalArgumentException("Mangler termindato i :" + t);
        }
        if (t.erSøktOmTermin() && t.terminbekreftelseUtstedtDato() == null) {
            throw new IllegalArgumentException("Mangler utstedt dato i :" + t);
        }

        if (t.erSøktOmTermin() && t.erTerminbekreftelseUtstedtEtterTidligsteDato()) {
            return ja();
        }
        return nei(IKKE_OPPFYLT_GYLDIG_TERMINBEKREFTELSE_DATO, t.terminbekreftelseUtstedtDato(), t.terminbekreftelseTermindato());
    }
}
