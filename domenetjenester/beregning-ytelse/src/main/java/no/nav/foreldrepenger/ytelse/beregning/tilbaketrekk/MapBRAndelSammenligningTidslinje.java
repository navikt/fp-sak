package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.util.FPDateUtil;

class MapBRAndelSammenligningTidslinje {
    private MapBRAndelSammenligningTidslinje() {
        // skjul public constructor
    }

    static LocalDateTimeline<BRAndelSammenligning> opprettTidslinje(List<BeregningsresultatPeriode> originalTYPerioder,
                                                                    List<BeregningsresultatPeriode> revurderingTYPerioder) {
        LocalDateTimeline<List<BeregningsresultatAndel>> alleredeUtbetalt = lagAlleredeUtbetaltTidslinje(originalTYPerioder);
        LocalDateTimeline<List<BeregningsresultatAndel>> bgTidslinje = lagTidslinje(revurderingTYPerioder);
        return bgTidslinje.combine(alleredeUtbetalt, MapBRAndelSammenligningTidslinje::combine, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    private static LocalDateTimeline<List<BeregningsresultatAndel>> lagAlleredeUtbetaltTidslinje(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        LocalDateTimeline<List<BeregningsresultatAndel>> forrigeTYTidslinje = lagTidslinje(beregningsresultatPerioder);
        LocalDateTimeline<List<BeregningsresultatAndel>> alleredeUtbetalt = identifiserUtbetaltPeriode();
        return forrigeTYTidslinje.intersection(alleredeUtbetalt);
    }

    private static LocalDateTimeline<List<BeregningsresultatAndel>> identifiserUtbetaltPeriode() {
        LocalDate alleredeUtbetaltTom = FinnAlleredeUtbetaltTom.finn(FPDateUtil.iDag());
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
            .collect(Collectors.toList())
        );
    }

    private static LocalDateSegment<BRAndelSammenligning> combine(LocalDateInterval interval,
                                                                  LocalDateSegment<List<BeregningsresultatAndel>> bgSegment,
                                                                  LocalDateSegment<List<BeregningsresultatAndel>> forrigeSegment) {
        List<BeregningsresultatAndel> forrigeAndeler = Optional.ofNullable(forrigeSegment)
            .map(LocalDateSegment::getValue)
            .orElse(Collections.emptyList());
        List<BeregningsresultatAndel> bgAndeler = Optional.ofNullable(bgSegment)
            .map(LocalDateSegment::getValue)
            .orElse(Collections.emptyList());
        BRAndelSammenligning wrapper = new BRAndelSammenligning(forrigeAndeler, bgAndeler);
        return new LocalDateSegment<>(interval, wrapper);
    }
}
