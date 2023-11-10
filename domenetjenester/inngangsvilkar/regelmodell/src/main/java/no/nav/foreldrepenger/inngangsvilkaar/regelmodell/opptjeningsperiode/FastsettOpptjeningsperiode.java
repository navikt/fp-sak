package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode;

import java.util.HashMap;
import java.util.Map;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(FastsettOpptjeningsperiode.ID)
public class FastsettOpptjeningsperiode extends LeafSpecification<OpptjeningsperiodeMellomregning> {

    static final String ID = "FP_VK 21.9";
    static final String BESKRIVELSE = "Regnes relativt til Skjæringstidspunkt fra konfig til dagen før (siste dag i opptjeningsperioden)";

    private static final String OPPTJENINGSPERIODE_FOM = "OpptjeningsperiodeFOM";
    private static final String OPPTJENINGSPERIODE_TOM = "OpptjeningsperiodeTOM";

    public FastsettOpptjeningsperiode() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(OpptjeningsperiodeMellomregning regelmodell) {
        regelmodell.setOpptjeningsperiodeTom(regelmodell.getSkjæringsdatoOpptjening().minusDays(1));
        regelmodell.setOpptjeningsperiodeFom(regelmodell.getSkjæringsdatoOpptjening().minus(regelmodell.getRegelParametre().periodeLengde()));

        Map<String, Object> resultater = new HashMap<>();
        resultater.put(OPPTJENINGSPERIODE_FOM, String.valueOf(regelmodell.getOpptjeningsperiodeFom()));
        resultater.put(OPPTJENINGSPERIODE_TOM, String.valueOf(regelmodell.getOpptjeningsperiodeTom()));
        return beregnet(resultater);
    }
}
