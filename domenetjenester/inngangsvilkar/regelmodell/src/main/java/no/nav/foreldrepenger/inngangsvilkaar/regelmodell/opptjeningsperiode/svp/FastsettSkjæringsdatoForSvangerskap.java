package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.svp;

import java.util.HashMap;
import java.util.Map;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsperiodeGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(FastsettSkjæringsdatoForSvangerskap.ID)
public class FastsettSkjæringsdatoForSvangerskap extends LeafSpecification<OpptjeningsperiodeGrunnlag> {

    static final String ID = "FP_VK 21.5";
    static final String BESKRIVELSE = "opptjeningsvilkar for beregning settes til første dag etter siste aktivitetsdag";

    FastsettSkjæringsdatoForSvangerskap() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(OpptjeningsperiodeGrunnlag regelmodell) {
        regelmodell.setSkjæringsdatoOpptjening(regelmodell.getFørsteUttaksDato());

        Map<String, Object> resultater = new HashMap<>();
        resultater.put("skjæringstidspunktOpptjening", String.valueOf(regelmodell.getSkjæringsdatoOpptjening()));
        return beregnet(resultater);
    }
}
