package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeMellomregning;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkAdopsjon.ID)
public class SjekkAdopsjon extends LeafSpecification<OpptjeningsperiodeMellomregning> {

    static final String ID = "FP_VK 21.10";
    static final String BESKRIVELSE = "Gjelder det adopsjon?";

    SjekkAdopsjon() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(OpptjeningsperiodeMellomregning regelmodell) {
        return FagsakÅrsak.ADOPSJON.equals(regelmodell.getGrunnlag().fagsakÅrsak()) ? ja() : nei();
    }
}
