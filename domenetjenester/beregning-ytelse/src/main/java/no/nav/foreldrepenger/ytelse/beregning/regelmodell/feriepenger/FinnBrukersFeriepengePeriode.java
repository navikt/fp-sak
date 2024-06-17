package no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Dekningsgrad;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.vedtak.konfig.Tid;

class FinnBrukersFeriepengePeriode extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.3";
    public static final String BESKRIVELSE = "Finner brukers feriepengeperiode.";


    FinnBrukersFeriepengePeriode() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        var beregningsresultatPerioder = regelModell.getBeregningsresultatPerioder();
        var annenPartsBeregningsresultatPerioder = regelModell.getAnnenPartsBeregningsresultatPerioder();
        var erForelder1 = regelModell.erForelder1();
        var feriepengePeriodeFom = finnFørsteUttaksdagArbeidstaker(beregningsresultatPerioder, annenPartsBeregningsresultatPerioder);
        var feriepengePeriodeTom = finnFeriepengerPeriodeTom(regelModell, feriepengePeriodeFom, erForelder1);

        regelModell.setFeriepengerPeriode(feriepengePeriodeFom, feriepengePeriodeTom);

        //Regelsporing
        Map<String, Object> resultater = new LinkedHashMap<>();
        resultater.put("FeriepengePeriode.fom", feriepengePeriodeFom);
        resultater.put("FeriepengePeriode.tom", feriepengePeriodeTom);
        return beregnet(resultater);
    }

    private LocalDate finnFeriepengerPeriodeTom(BeregningsresultatFeriepengerRegelModell regelModell,
                                                LocalDate feriepengePeriodeFom,
                                                boolean erForelder1) {
        var beregningsresultatPerioder = regelModell.getBeregningsresultatPerioder();
        var annenPartsBeregningsresultatPerioder = regelModell.getAnnenPartsBeregningsresultatPerioder();
        var maksAntallDager = antallDagerFeriepenger(regelModell.getDekningsgrad(), regelModell.getAntallDagerFeriepenger());
        var annenpartRettPåFeriepenger = regelModell.getInntektskategorierAnnenPart().stream().anyMatch(Inntektskategori::erArbeidstakerEllerSjømann);
        var sisteUttaksdag = finnSisteUttaksdag(beregningsresultatPerioder, annenPartsBeregningsresultatPerioder);
        var antallDager = 0;

        for (var dato = feriepengePeriodeFom; !dato.isAfter(sisteUttaksdag); dato = dato.plusDays(1)) {
            var antallDagerSomLeggesTilFeriepengeperioden = finnAntallDagerSomSkalLeggesTil(beregningsresultatPerioder,
                annenPartsBeregningsresultatPerioder, annenpartRettPåFeriepenger, dato);
            antallDager += antallDagerSomLeggesTilFeriepengeperioden;
            if (antallDager == maksAntallDager) {
                return dato;
            }
            if (antallDager > maksAntallDager) {
                return erForelder1 ? dato : dato.minusDays(1);
            }
        }
        return sisteUttaksdag;
    }

    private int antallDagerFeriepenger(Dekningsgrad dekningsgrad, int antallDagerFeriepenger) {
        return switch (dekningsgrad) {
            case DEKNINGSGRAD_100 -> antallDagerFeriepenger;
            case DEKNINGSGRAD_80 -> (int) (antallDagerFeriepenger / dekningsgrad.getVerdi());
        };
    }

    private int finnAntallDagerSomSkalLeggesTil(List<BeregningsresultatPeriode> beregningsresultatPerioder,
                                                List<BeregningsresultatPeriode> annenPartsBeregningsresultatPerioder,
                                                boolean annenpartRettPåFeriepenger,
                                                LocalDate dato) {
        if (erHelg(dato)) {
            return 0;
        }
        var brukerHarUttakDager = harUttak(beregningsresultatPerioder, dato) ? 1 : 0;
        var annenpartUttakOgRettDager = annenpartRettPåFeriepenger && harUttak(annenPartsBeregningsresultatPerioder, dato) ? 1 : 0;
        return brukerHarUttakDager + annenpartUttakOgRettDager;
    }

    private boolean harUttak(List<BeregningsresultatPeriode> beregningsresultatPerioder, LocalDate dato) {
        return beregningsresultatPerioder.stream()
            .filter(p -> p.inneholder(dato))
            .flatMap(beregningsresultatPeriode -> beregningsresultatPeriode.getBeregningsresultatAndelList().stream())
            .anyMatch(andel -> andel.getDagsats() > 0);
    }

    private boolean erHelg(LocalDate dato) {
        return dato.getDayOfWeek().getValue() > DayOfWeek.FRIDAY.getValue();
    }

    private LocalDate finnFørsteUttaksdagArbeidstaker(List<BeregningsresultatPeriode> beregningsresultatPerioder,
                                                      List<BeregningsresultatPeriode> annenPartsBeregningsresultatPerioder) {
        var førsteUttaksdagBruker = finnFørsteUttaksdagArbeidstaker(beregningsresultatPerioder).orElseThrow(
            () -> new IllegalStateException("Fant ingen perioder med utbetaling for bruker"));
        var førsteUttaksdagAnnenPart = finnFørsteUttaksdagArbeidstaker(annenPartsBeregningsresultatPerioder).orElse(Tid.TIDENES_ENDE);
        return førsteUttaksdagBruker.isBefore(førsteUttaksdagAnnenPart) ? førsteUttaksdagBruker : førsteUttaksdagAnnenPart;
    }

    static Optional<LocalDate> finnFørsteUttaksdagArbeidstaker(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        return beregningsresultatPerioder.stream()
            .filter(periode -> finnesAndelMedKravPåFeriepengerOgUtbetaling(periode.getBeregningsresultatAndelList()))
            .map(BeregningsresultatPeriode::getFom)
            .min(Comparator.naturalOrder());
    }

    static private boolean finnesAndelMedKravPåFeriepengerOgUtbetaling(List<BeregningsresultatAndel> andeler) {
        return andeler.stream().filter(andel -> andel.getInntektskategori().erArbeidstakerEllerSjømann()).anyMatch(andel -> andel.getDagsats() > 0);

    }

    private LocalDate finnSisteUttaksdag(List<BeregningsresultatPeriode> beregningsresultatPerioder,
                                         List<BeregningsresultatPeriode> annenPartsBeregningsresultatPerioder) {
        var sisteUttaksdagBruker = finnSisteUttaksdag(beregningsresultatPerioder).orElseThrow(
            () -> new IllegalStateException("Fant ingen perioder med utbetaling for bruker"));
        var sisteUttaksdagAnnenPart = finnSisteUttaksdag(annenPartsBeregningsresultatPerioder).orElse(Tid.TIDENES_BEGYNNELSE);
        return sisteUttaksdagBruker.isAfter(sisteUttaksdagAnnenPart) ? sisteUttaksdagBruker : sisteUttaksdagAnnenPart;
    }

    private Optional<LocalDate> finnSisteUttaksdag(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        return beregningsresultatPerioder.stream()
            .filter(periode -> periode.getBeregningsresultatAndelList().stream().anyMatch(andel -> andel.getDagsats() > 0))
            .map(BeregningsresultatPeriode::getTom)
            .max(Comparator.naturalOrder());
    }
}
