package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;
import no.nav.fpsak.nare.specification.LeafSpecification;

/**
 * Regel definerer hvorvidt svangerskapsuke X (p.t. 22) er passert ifht. behandlingsdato
 */
public class SjekkBehandlingsdatoPassertXSvangerskapsUker extends LeafSpecification<FødselsvilkårGrunnlag> {

    static final String ID = SjekkBehandlingsdatoPassertXSvangerskapsUker.class.getSimpleName();

    static final RuleReasonRefImpl IKKE_OPPFYLT_PASSERT_TIDLIGSTE_SVANGERSKAPSUKE_KAN_BEHANDLES = new RuleReasonRefImpl("1001",
            "Behandlingsdato {0} før svangerskapsuke 22 (termindato ({1}))");

    SjekkBehandlingsdatoPassertXSvangerskapsUker() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(FødselsvilkårGrunnlag t) {

        if (t.erSøktOmTermin() && t.terminbekreftelseTermindato() == null) {
            throw new IllegalArgumentException("Mangler termindato i :" + t);
        }
        if (t.behandlingsdato() == null) {
            throw new IllegalArgumentException("Mangler behandlingsdato i :" + t);
        }

        if (t.erBehandlingsdatoEtterTidligsteDato()) {
            return ja();
        }
        return nei(IKKE_OPPFYLT_PASSERT_TIDLIGSTE_SVANGERSKAPSUKE_KAN_BEHANDLES, t.behandlingsdato(), t.terminbekreftelseTermindato());
    }
    @Override
    public String beskrivelse() {
        return "Sjekk behandlingsdato i 22 svangerskapsuke";
    }
}
