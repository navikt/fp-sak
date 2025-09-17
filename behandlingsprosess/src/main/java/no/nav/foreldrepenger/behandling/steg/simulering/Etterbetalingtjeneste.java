package no.nav.foreldrepenger.behandling.steg.simulering;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.ytelse.beregning.Virkedager;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.vedtak.konfig.Tid;

public class Etterbetalingtjeneste {
    private static final int UTBETALINGS_DATO = 18;
    private static final BigDecimal STANDARD_BELØPSGRENSE = BigDecimal.valueOf(30_000);

    private Etterbetalingtjeneste() {
        // Skjuler default konstruktør
    }


    /**
     * Tjeneste som lager tidslinje over tidligere og nytt beregningsresultat og sjekker om det vil bli etterbetalt for perioder som allerede er utbetalt i tidligere behandling
     * og om etterbetalingssum vil overskride et gitt beløp
     *
     * @param dagensDato             taes inn som parameter for å kunne skrive bedre tester
     * @param forrigeRes
     * @param nyttBeregningsresultat
     * @return
     */
    public static EtterbetalingskontrollResultat finnSumSomVilBliEtterbetalt(LocalDate dagensDato, BeregningsresultatEntitet forrigeRes, BeregningsresultatEntitet nyttBeregningsresultat) {
        return finnSumSomVilBliEtterbetalt(dagensDato, forrigeRes, nyttBeregningsresultat, STANDARD_BELØPSGRENSE);
    }

    public static EtterbetalingskontrollResultat finnSumSomVilBliEtterbetalt(LocalDate dagensDato, BeregningsresultatEntitet forrigeRes, BeregningsresultatEntitet nyttBeregningsresultat, BigDecimal beløpsgrense) {
        var sisteDagSomErUtbetalt = finnSisteUtbetalteDato(dagensDato);

        var originalUtbetaltTidslinje = lagUtbetaltTidslinje(sisteDagSomErUtbetalt, forrigeRes);
        var nyUtbetaltTidslinje = lagUtbetaltTidslinje(sisteDagSomErUtbetalt, nyttBeregningsresultat);

        var overlappendeNyTidslinje = nyUtbetaltTidslinje.intersection(originalUtbetaltTidslinje);

        var originalUtbetaling = BigDecimal.valueOf(summerDagsats(originalUtbetaltTidslinje));
        var nyUtbetaling = BigDecimal.valueOf(summerDagsats(overlappendeNyTidslinje));
        var etterbetalingssum = nyUtbetaling.subtract(originalUtbetaling).max(BigDecimal.ZERO);
        return new EtterbetalingskontrollResultat(etterbetalingssum, etterbetalingssum.compareTo(beløpsgrense) > 0);
    }

    private static int summerDagsats(LocalDateTimeline<Integer> tidslinje) {
        return tidslinje.stream().mapToInt(segment -> {
            var virkedager = Virkedager.beregnAntallVirkedager(segment.getFom(), segment.getTom());
            return virkedager * segment.getValue();
        }).sum();
    }

    private static LocalDate finnSisteUtbetalteDato(LocalDate dagensDato) {
        if (dagensDato.getDayOfMonth() < UTBETALINGS_DATO) {
            return dagensDato.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        }
        return dagensDato.with(TemporalAdjusters.lastDayOfMonth());
    }

    private static LocalDateTimeline<Integer> lagUtbetaltTidslinje(LocalDate sisteDagSomErUtbetalt, BeregningsresultatEntitet forrigeRes) {
        var segmenter = forrigeRes.getBeregningsresultatPerioder()
            .stream()
            .filter(p -> p.getDagsats() > 0)
            .map(p -> new LocalDateSegment<>(p.getBeregningsresultatPeriodeFom(), p.getBeregningsresultatPeriodeTom(), finnSumDirekteutbetaling(p.getBeregningsresultatAndelList())))
            .toList();
        var beregningsresultatTidslinje = new LocalDateTimeline<>(segmenter);
        var utbetaltPeriodeTidslinje = new LocalDateTimeline<>(Tid.TIDENES_BEGYNNELSE, sisteDagSomErUtbetalt, null);
        return beregningsresultatTidslinje.intersection(utbetaltPeriodeTidslinje);
    }

    private static int finnSumDirekteutbetaling(List<BeregningsresultatAndel> beregningsresultatAndelList) {
        return beregningsresultatAndelList.stream()
            .filter(BeregningsresultatAndel::erBrukerMottaker)
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();
    }
}
