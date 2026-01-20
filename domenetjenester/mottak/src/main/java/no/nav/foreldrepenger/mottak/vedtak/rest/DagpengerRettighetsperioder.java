package no.nav.foreldrepenger.mottak.vedtak.rest;

import java.time.LocalDate;
import java.util.List;

public record DagpengerRettighetsperioder(String personIdent, List<Rettighetsperiode> perioder) {

    public record Rettighetsperiode(LocalDate fraOgMedDato, LocalDate tilOgMedDato, DagpengerKilde kilde) {
    }

    public enum DagpengerKilde { DP_SAK, ARENA }
}

