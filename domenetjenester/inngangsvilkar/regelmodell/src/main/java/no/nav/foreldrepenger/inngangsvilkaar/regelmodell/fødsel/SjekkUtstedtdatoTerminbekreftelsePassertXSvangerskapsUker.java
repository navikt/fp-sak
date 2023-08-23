package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelUtfallMerknad;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

public class SjekkUtstedtdatoTerminbekreftelsePassertXSvangerskapsUker extends LeafSpecification<FødselsvilkårGrunnlag> {

    static final String ID = SjekkUtstedtdatoTerminbekreftelsePassertXSvangerskapsUker.class.getSimpleName();

    static final MerknadRuleReasonRef IKKE_OPPFYLT_GYLDIG_TERMINBEKREFTELSE_DATO =
        new MerknadRuleReasonRef(RegelUtfallMerknad.RVM_1019, "Terminbekreftelse utstedt før 22 svangerskapsuke (termindato ({0}))");

    SjekkUtstedtdatoTerminbekreftelsePassertXSvangerskapsUker() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(FødselsvilkårGrunnlag t) {
        if (t.erSøktOmTermin() && t.terminbekreftelseTermindato() == null) {
            throw new IllegalArgumentException("Mangler termindato i :" + t);
        }

        if (t.erSøktOmTermin() && t.erTerminbekreftelseUtstedtEtterTidligsteDato()) {
            return ja();
        }
        return nei(IKKE_OPPFYLT_GYLDIG_TERMINBEKREFTELSE_DATO, t.terminbekreftelseTermindato());
    }
}
