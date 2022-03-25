package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeMellomregning;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkFødsel.ID)
public class SjekkFødsel extends LeafSpecification<OpptjeningsperiodeMellomregning> {

    static final String ID = "FP_VK 21.1";
    static final String BESKRIVELSE = "Gjelder det fødsel?";

    SjekkFødsel() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(OpptjeningsperiodeMellomregning regelmodell) {
        return FagsakÅrsak.FØDSEL.equals(regelmodell.getGrunnlag().fagsakÅrsak()) ? ja() : nei();
    }
}
