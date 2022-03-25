package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

        var terminDato = Optional.ofNullable(regelmodell.getGrunnlag().terminDato());
        var tidligsteUttakFørTermin = terminDato.map(t -> t.minus(regelmodell.getRegelParametre().annenTidligsteUttakFørTerminPeriode()));
        var hendelsesDato = regelmodell.getGrunnlag().hendelsesDato();

        var tidligsteUttakDato = tidligsteUttakFørTermin.isPresent() && tidligsteUttakFørTermin.get().isBefore(hendelsesDato) ? tidligsteUttakFørTermin.get() : hendelsesDato;

        if (skjæringsDatoOpptjening.isBefore(tidligsteUttakDato)) {
            skjæringsDatoOpptjening = tidligsteUttakDato;
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
