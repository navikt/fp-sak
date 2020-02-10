package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsperiodeGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(SjekkOmsorg.ID)
public class SjekkOmsorg extends LeafSpecification<OpptjeningsperiodeGrunnlag> {

    static final String ID = "FP_VK 21.11";
    static final String BESKRIVELSE = "Gjelder det en person som har eller får tildelt foreldeansvar når en av foreldrene dør?";

    SjekkOmsorg() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(OpptjeningsperiodeGrunnlag regelmodell) {
        return regelmodell.getFagsakÅrsak().equals(FagsakÅrsak.OMSORG) ? ja() : nei();
    }
}
