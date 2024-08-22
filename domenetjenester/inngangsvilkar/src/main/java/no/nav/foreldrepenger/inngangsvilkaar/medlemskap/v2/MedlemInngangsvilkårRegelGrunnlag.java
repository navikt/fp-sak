package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateInterval;

record MedlemInngangsvilkårRegelGrunnlag(LocalDateInterval vurderingsperiodeBosatt,
                                         LocalDateInterval vurderingsperiodeLovligOpphold,
                                         Set<RegisterMedlemskapBeslutning> registrertMedlemskapBeslutning,
                                         Personopplysninger personopplysninger,
                                         Søknad søknad) {

    record Søknad(Set<LocalDateInterval> utenlandsopphold) {
    }
}
