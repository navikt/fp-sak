package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRef;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkBrukerErAvklartSomIkkeBosatt.ID)
public class SjekkBrukerErAvklartSomIkkeBosatt extends LeafSpecification<MedlemskapsvilkårGrunnlag> {

    static final String ID = "FP_VK_2.x";  //TODO FL Hva skal stå her?

    static final RuleReasonRef IKKE_OPPFYLT_BRUKER_ER_AVKLART_SOM_IKKE_BOSATT = new RuleReasonRefImpl("1025", "Bruker er avklart som ikke bosatt.");


    SjekkBrukerErAvklartSomIkkeBosatt() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(MedlemskapsvilkårGrunnlag grunnlag) {
        if (!grunnlag.isBrukerAvklartBosatt()) {
            return ja(IKKE_OPPFYLT_BRUKER_ER_AVKLART_SOM_IKKE_BOSATT);
        }
        return nei();
    }
}
