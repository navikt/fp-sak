package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeMellomregning;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

class IkkeGyldigUtgang extends LeafSpecification<OpptjeningsperiodeMellomregning> {

    static final String ID = "FP_VK 21";
    static final String BESKRIVELSE = "Ikke gyldig utgang";

    IkkeGyldigUtgang() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(OpptjeningsperiodeMellomregning regelmodell) {
        return nei();
    }
}

