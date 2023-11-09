package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.vedtak.konfig.Tid;

class MapBRAndelSammenligningTidslinje {
    private MapBRAndelSammenligningTidslinje() {
        // skjul public constructor
    }

    static LocalDateTimeline<BRAndelSammenligning> opprettTidslinje(List<BeregningsresultatPeriode> originalTYPerioder,
            List<BeregningsresultatPeriode> revurderingTYPerioder) {
        var alleredeUtbetalt = lagAlleredeUtbetaltTidslinje(LocalDate.now(), originalTYPerioder);
        var bgTidslinje = lagTidslinje(revurderingTYPerioder);
        return bgTidslinje.combine(alleredeUtbetalt, MapBRAndelSammenligningTidslinje::combine, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    static LocalDateTimeline<BRAndelSammenligning> opprettTidslinjeTest(List<BeregningsresultatPeriode> originalTYPerioder,
                                                                    List<BeregningsresultatPeriode> revurderingTYPerioder,
                                                                        LocalDate dagensDato) {
        var alleredeUtbetalt = lagAlleredeUtbetaltTidslinje(dagensDato, originalTYPerioder);
        var bgTidslinje = lagTidslinje(revurderingTYPerioder);
        return bgTidslinje.combine(alleredeUtbetalt, MapBRAndelSammenligningTidslinje::combine, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    private static LocalDateTimeline<List<BeregningsresultatAndel>> lagAlleredeUtbetaltTidslinje(LocalDate dagensDato,
            List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        var forrigeTYTidslinje = lagTidslinje(beregningsresultatPerioder);
        var alleredeUtbetalt = identifiserUtbetaltPeriode(dagensDato);
        return forrigeTYTidslinje.intersection(alleredeUtbetalt);
    }

    private static LocalDateTimeline<List<BeregningsresultatAndel>> identifiserUtbetaltPeriode(LocalDate dagensDato) {
        var alleredeUtbetaltTom = FinnAlleredeUtbetaltTom.finn(dagensDato);
        return new LocalDateTimeline<>(
                Tid.TIDENES_BEGYNNELSE,
                alleredeUtbetaltTom,
                Collections.emptyList());
    }

    private static LocalDateTimeline<List<BeregningsresultatAndel>> lagTidslinje(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        return new LocalDateTimeline<>(beregningsresultatPerioder.stream()
                .sorted(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom))
                .map(p -> new LocalDateSegment<>(
                        p.getBeregningsresultatPeriodeFom(),
                        p.getBeregningsresultatPeriodeTom(),
                        p.getBeregningsresultatAndelList()))
                .toList());
    }

    private static LocalDateSegment<BRAndelSammenligning> combine(LocalDateInterval interval,
            LocalDateSegment<List<BeregningsresultatAndel>> bgSegment,
            LocalDateSegment<List<BeregningsresultatAndel>> forrigeSegment) {
        var forrigeAndeler = Optional.ofNullable(forrigeSegment)
                .map(LocalDateSegment::getValue)
                .orElse(Collections.emptyList());
        var bgAndeler = Optional.ofNullable(bgSegment)
                .map(LocalDateSegment::getValue)
                .orElse(Collections.emptyList());
        var wrapper = new BRAndelSammenligning(forrigeAndeler, bgAndeler);
        return new LocalDateSegment<>(interval, wrapper);
    }
}
