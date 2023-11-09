package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeMellomregning;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

@RuleDocumentation(FastsettSkjæringsdatoMorFødsel.ID)
public class FastsettSkjæringsdatoMorFødsel extends LeafSpecification<OpptjeningsperiodeMellomregning> {

    static final String ID = "FP_VK 21.5";
    static final String BESKRIVELSE = "opptjeningsvilkar for beregning settes til første dag etter siste aktivitetsdag";

    FastsettSkjæringsdatoMorFødsel() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(OpptjeningsperiodeMellomregning regelmodell) {
        var skjæringsDatoOpptjening = regelmodell.getGrunnlag().førsteUttaksDato();

        var terminDato = Optional.ofNullable(regelmodell.getGrunnlag().terminDato());
        var tidligsteUttakFørTermin = terminDato.map(t -> t.minus(regelmodell.getRegelParametre().morTidligsteUttakFørTerminPeriode()));
        var senesteUttakFørTermin = terminDato.map(t -> t.minus(regelmodell.getRegelParametre().morSenesteUttakFørTerminPeriode()));
        var hendelsesDato = regelmodell.getGrunnlag().hendelsesDato();
        // Strengt tatt ikke i lov eller rundskriv, men er etablert og gammel praksis med 3 uker før dersom termindato mangler.
        var tidligsteUttakFørHendelseSedvane = hendelsesDato.minus(regelmodell.getRegelParametre().morSenesteUttakFørTerminPeriode());

        var tidligsteUttakDato = tidligsteUttakFørTermin.isPresent() && tidligsteUttakFørTermin.get().isBefore(tidligsteUttakFørHendelseSedvane)
            ? tidligsteUttakFørTermin.get() : tidligsteUttakFørHendelseSedvane;

        if (skjæringsDatoOpptjening.isBefore(tidligsteUttakDato)) {
            skjæringsDatoOpptjening = tidligsteUttakDato;
        }

        if (senesteUttakFørTermin.isPresent() && skjæringsDatoOpptjening.isAfter(senesteUttakFørTermin.get())) {
            skjæringsDatoOpptjening = senesteUttakFørTermin.get();
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
