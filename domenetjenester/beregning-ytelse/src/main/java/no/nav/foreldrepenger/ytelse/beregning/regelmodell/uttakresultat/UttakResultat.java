package no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.util.List;

public record UttakResultat(List<UttakResultatPeriode> uttakResultatPerioder) {

    public LocalDateTimeline<UttakResultatPeriode> getUttakPeriodeTimeline() {
        var uttaksPerioder = uttakResultatPerioder().stream()
            .map(periode -> new LocalDateSegment<>(periode.fom(), periode.tom(), periode))
            .toList();
        return new LocalDateTimeline<>(uttaksPerioder);
    }

}
