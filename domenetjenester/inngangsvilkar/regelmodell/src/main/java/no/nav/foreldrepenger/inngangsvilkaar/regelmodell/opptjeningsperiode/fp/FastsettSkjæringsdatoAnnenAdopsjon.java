package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp;

import java.util.HashMap;
import java.util.Map;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeMellomregning;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(FastsettSkjæringsdatoAnnenAdopsjon.ID)
public class FastsettSkjæringsdatoAnnenAdopsjon extends LeafSpecification<OpptjeningsperiodeMellomregning> {

    static final String ID = "FP_VK 21.8";
    static final String BESKRIVELSE = "Første uttaksdag, men aldri senere enn dagen for ankomst Norge ved adopsjon fra utlandet";

    FastsettSkjæringsdatoAnnenAdopsjon() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(OpptjeningsperiodeMellomregning regelmodell) {
        var skjæringsdatoOpptjening = regelmodell.getGrunnlag().førsteUttaksDato();

        var hendelsesDato = regelmodell.getGrunnlag().hendelsesDato();
        if(skjæringsdatoOpptjening.isBefore(hendelsesDato)) {
            skjæringsdatoOpptjening = hendelsesDato;
        }

        regelmodell.setSkjæringsdatoOpptjening(skjæringsdatoOpptjening);

        Map<String, Object> resultater = new HashMap<>();
        resultater.put("skjæringstidspunktOpptjening", String.valueOf(regelmodell.getSkjæringsdatoOpptjening()));
        return beregnet(resultater);
    }
}
