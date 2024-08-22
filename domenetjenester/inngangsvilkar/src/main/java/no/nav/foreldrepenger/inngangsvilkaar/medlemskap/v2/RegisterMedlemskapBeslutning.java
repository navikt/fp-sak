package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import no.nav.fpsak.tidsserie.LocalDateInterval;

import java.time.LocalDate;

record RegisterMedlemskapBeslutning(LocalDateInterval interval, LocalDate beslutningsdato) {
}
