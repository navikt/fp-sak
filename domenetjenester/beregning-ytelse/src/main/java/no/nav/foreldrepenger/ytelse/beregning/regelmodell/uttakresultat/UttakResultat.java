package no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public record UttakResultat(List<UttakResultatPeriode> uttakResultatPerioder) {

    public LocalDateTimeline<UttakResultatPeriode> getUttakPeriodeTimeline() {
        var uttaksPerioder = uttakResultatPerioder().stream()
            .map(periode -> new LocalDateSegment<>(periode.fom(), periode.tom(), periode))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(uttaksPerioder);
    }

}
