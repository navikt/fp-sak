package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.abakus.vedtak.ytelse.Desimaltall;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

public class SjekkOverlapp {
    private static final BigDecimal HUNDRE = new BigDecimal(100);

    private SjekkOverlapp() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean erOverlappOgMerEnn100Prosent(Optional<BeregningsresultatEntitet> beregningsresultat, List<YtelseV1> ytelser) {
        var ytelseSegments = ytelser.stream()
            .flatMap(y -> y.getAnvist().stream())
            .map(p -> new LocalDateSegment<>(p.getPeriode().getFom(), p.getPeriode().getTom(), utbetalingsgradHundreHvisNull(p.getUtbetalingsgrad())))
            .filter(s -> s.getValue().compareTo(BigDecimal.ZERO) > 0)
            .toList();

        if (ytelseSegments.isEmpty()) {
            return false;
        }

        var minYtelseDato = ytelseSegments.stream().map(LocalDateSegment::getFom).min(Comparator.naturalOrder()).orElseThrow();
        var ytelseTidslinje = new LocalDateTimeline<>(ytelseSegments, StandardCombinators::max);

        var tilkjentTidslinje = beregningsresultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder)
            .orElse(List.of())
            .stream()
            .filter(p -> p.getDagsats() > 0)
            .filter(p -> p.getBeregningsresultatPeriodeTom().isAfter(minYtelseDato.minusDays(1)))
            .map(p -> new LocalDateSegment<>(p.getBeregningsresultatPeriodeFom(), p.getBeregningsresultatPeriodeTom(),
                p.getKalkulertUtbetalingsgrad()))
            .collect(Collectors.collectingAndThen(Collectors.toList(), v -> new LocalDateTimeline<>(v, StandardCombinators::max)));


        return !tilkjentTidslinje.intersection(ytelseTidslinje, StandardCombinators::sum).filterValue(v -> v.compareTo(HUNDRE) > 0).isEmpty();
    }

    private static BigDecimal utbetalingsgradHundreHvisNull(Desimaltall anvistUtbetalingsprosent) {
        return anvistUtbetalingsprosent != null && anvistUtbetalingsprosent.getVerdi() != null ? anvistUtbetalingsprosent.getVerdi() : HUNDRE;
    }
}
