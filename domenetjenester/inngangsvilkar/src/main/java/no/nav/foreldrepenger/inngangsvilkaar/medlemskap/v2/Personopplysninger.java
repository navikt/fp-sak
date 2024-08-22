package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateInterval;

record Personopplysninger(Set<RegionPeriode> regioner,
                          Set<LocalDateInterval> oppholdstillatelser,
                          Set<PersonstatusPeriode> personstatus,
                          Set<Adresse> adresser) {

    record RegionPeriode(LocalDateInterval periode, Region region) {
    }

    enum Region {
        NORDEN,
        EØS,
        TREDJELAND
    }

    record PersonstatusPeriode(LocalDateInterval interval, PersonstatusPeriode.Type type) {
        enum Type {
            D_NUMMER,
            BOSATT_ETTER_FOLKEREGISTERLOVEN,
            IKKE_BOSATT,
            DØD,
            FORSVUNNET,
            OPPHØRT
        }
    }

    record Adresse(LocalDateInterval periode, Type type, boolean erUtenlandsk) {

        enum Type {
            BOSTEDSADRESSE,
            POSTADRESSE,
            POSTADRESSE_UTLAND,
            MIDLERTIDIG_POSTADRESSE_NORGE,
            MIDLERTIDIG_POSTADRESSE_UTLAND,
            UKJENT_ADRESSE,
        }
    }
}
