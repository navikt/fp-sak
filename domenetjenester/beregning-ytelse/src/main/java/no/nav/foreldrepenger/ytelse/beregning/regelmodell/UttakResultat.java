package no.nav.foreldrepenger.ytelse.beregning.regelmodell;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class UttakResultat {
    private List<UttakResultatPeriode> uttakResultatPerioder;

    public UttakResultat(List<UttakResultatPeriode> uttakResultatPerioder) {
        this.uttakResultatPerioder = uttakResultatPerioder;
    }

    public List<UttakResultatPeriode> getUttakResultatPerioder() {
        return uttakResultatPerioder;
    }

    public LocalDateTimeline<UttakResultatPeriode> getUttakPeriodeTimeline() {
        List<LocalDateSegment<UttakResultatPeriode>> uttaksPerioder = uttakResultatPerioder.stream()
            .map(periode -> new LocalDateSegment<>(periode.getFom(), periode.getTom(), periode))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(uttaksPerioder);
    }
}
