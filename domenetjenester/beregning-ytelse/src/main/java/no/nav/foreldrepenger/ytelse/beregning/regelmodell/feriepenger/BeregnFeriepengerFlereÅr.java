package no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class BeregnFeriepengerFlereÅr extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.6";
    public static final String BESKRIVELSE = "Beregn feriepenger for periode som går over flere kalenderår.";


    BeregnFeriepengerFlereÅr() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        Map<String, Object> resultater = new LinkedHashMap<>();

        var feriepengerPeriode = regelModell.getFeriepengerPeriode();
        var feriepengerPerioderPrÅr = periodiserLocalDateIntervalPrÅr(feriepengerPeriode);

        feriepengerPerioderPrÅr.forEach(feriepengerPeriodePrÅr ->
            BeregnFeriepengerForPeriode.beregn(resultater, regelModell, feriepengerPeriodePrÅr)
        );

        return beregnet(resultater);
    }

    private static List<LocalDateInterval> periodiserLocalDateIntervalPrÅr(LocalDateInterval feriepengerPeriode) {
        var fom = feriepengerPeriode.getFomDato();
        var tom = feriepengerPeriode.getTomDato();
        List<LocalDateInterval> perioder = new ArrayList<>();
        while (fom.getYear() != tom.getYear()) {
            var sisteDagIÅr = fom.withMonth(12).withDayOfMonth(31);
            var dateInterval = new LocalDateInterval(fom, sisteDagIÅr);
            perioder.add(dateInterval);
            fom = sisteDagIÅr.plusDays(1);
        }
        var dateInterval = new LocalDateInterval(fom, tom);
        perioder.add(dateInterval);
        return perioder;
    }
}
