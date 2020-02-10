package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsperiodeGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkFødsel.ID)
public class SjekkFødsel extends LeafSpecification<OpptjeningsperiodeGrunnlag> {

    static final String ID = "FP_VK 21.1";
    static final String BESKRIVELSE = "Gjelder det fødsel?";

    SjekkFødsel() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(OpptjeningsperiodeGrunnlag regelmodell) {
        return FagsakÅrsak.FØDSEL.equals(regelmodell.getFagsakÅrsak()) ? ja() : nei();
    }
}
