package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp;

import java.util.HashMap;
import java.util.Map;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeMellomregning;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(FastsettSkjæringsdatoAnnenFødsel.ID)
public class FastsettSkjæringsdatoAnnenFødsel extends LeafSpecification<OpptjeningsperiodeMellomregning> {

    static final String ID = "FP_VK 21.6";
    static final String BESKRIVELSE = "Tidligste av: første uttaksdag, dagen etter mors seneste maxdato (fars første uttaksdag)";

    FastsettSkjæringsdatoAnnenFødsel() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(OpptjeningsperiodeMellomregning regelmodell) {
        var skjæringsDatoOpptjening = regelmodell.getGrunnlag().førsteUttaksDato();

        if (skjæringsDatoOpptjening.isBefore(regelmodell.getGrunnlag().hendelsesDato())) {
            skjæringsDatoOpptjening = regelmodell.getGrunnlag().hendelsesDato();
        }

        var morsMaksdato = regelmodell.getGrunnlag().morsMaksdatoOpt();
        if (morsMaksdato.isPresent()) {
            var førsteMuligeUttak = morsMaksdato.get().plusDays(1);
            if (skjæringsDatoOpptjening.isAfter(førsteMuligeUttak)) {
                skjæringsDatoOpptjening = førsteMuligeUttak;
            }
        }

        regelmodell.setSkjæringsdatoOpptjening(skjæringsDatoOpptjening);

        Map<String, Object> resultater = new HashMap<>();
        resultater.put("skjæringstidspunktOpptjening", String.valueOf(regelmodell.getSkjæringsdatoOpptjening()));
        return beregnet(resultater);
    }
}
