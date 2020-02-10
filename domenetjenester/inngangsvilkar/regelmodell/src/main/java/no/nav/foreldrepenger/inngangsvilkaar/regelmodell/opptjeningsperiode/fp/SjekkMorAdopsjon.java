package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.SoekerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsperiodeGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkMorAdopsjon.ID)
public class SjekkMorAdopsjon extends LeafSpecification<OpptjeningsperiodeGrunnlag> {

    static final String ID = "FP_VK 21.3";
    static final String BESKRIVELSE = "Er mor søker?";

    SjekkMorAdopsjon() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(OpptjeningsperiodeGrunnlag regelmodell) {
        return regelmodell.getSøkerRolle().equals(SoekerRolle.MORA) ? ja() : nei();
    }
}
