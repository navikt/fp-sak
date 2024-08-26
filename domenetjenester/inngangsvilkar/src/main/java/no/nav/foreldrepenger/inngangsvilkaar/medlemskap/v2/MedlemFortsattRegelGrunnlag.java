package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import java.time.LocalDate;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateInterval;

record MedlemFortsattRegelGrunnlag(LocalDateInterval vurderingsperiode,
                                   Set<RegisterMedlemskapBeslutning> registrertMedlemskapBeslutning,
                                   Personopplysninger personopplysninger,
                                   Arbeid arbeid) {

    record Arbeid(Set<LocalDateInterval> ansettelsePerioder) {
    }

    record RegisterMedlemskapBeslutning(LocalDateInterval interval, LocalDate beslutningsdato) {
    }
}
