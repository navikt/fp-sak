package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateInterval;

record MedlemFortsattRegelGrunnlag(LocalDateInterval vurderingsperiode,
                                   Set<RegisterMedlemskapBeslutning> registrertMedlemskapBeslutning,
                                   Personopplysninger personopplysninger) {
}
