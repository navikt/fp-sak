package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp;

import java.util.HashMap;
import java.util.Map;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsperiodeGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(FastsettSkjæringsdatoMorFødsel.ID)
public class FastsettSkjæringsdatoMorFødsel extends LeafSpecification<OpptjeningsperiodeGrunnlag> {

    static final String ID = "FP_VK 21.5";
    static final String BESKRIVELSE = "opptjeningsvilkar for beregning settes til første dag etter siste aktivitetsdag";

    FastsettSkjæringsdatoMorFødsel() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(OpptjeningsperiodeGrunnlag regelmodell) {
        var skjæringsDatoOpptjening = regelmodell.getFørsteUttaksDato();

        var terminDato = regelmodell.getTerminDato();
        var hendelsesDato = regelmodell.getHendelsesDato();

        var tidligsteUttakDato = hendelsesDato.minus(regelmodell.getTidligsteUttakFørFødselPeriode());
        if (terminDato != null && terminDato.isBefore(hendelsesDato)) {
            tidligsteUttakDato = terminDato.minus(regelmodell.getTidligsteUttakFørFødselPeriode());
        }

        if (skjæringsDatoOpptjening.isBefore(tidligsteUttakDato)) {
            skjæringsDatoOpptjening = tidligsteUttakDato;
        }

        if (terminDato != null && skjæringsDatoOpptjening.isAfter(terminDato.minusWeeks(3))) {
            skjæringsDatoOpptjening = terminDato.minusWeeks(3);
        }
        // Tilfelle fødsel mer enn tre uker før termindato
        if (skjæringsDatoOpptjening.isAfter(hendelsesDato)) {
            skjæringsDatoOpptjening = hendelsesDato;
        }
        regelmodell.setSkjæringsdatoOpptjening(skjæringsDatoOpptjening);

        Map<String, Object> resultater = new HashMap<>();
        resultater.put("skjæringstidspunktOpptjening", String.valueOf(regelmodell.getSkjæringsdatoOpptjening()));
        return beregnet(resultater);
    }
}
