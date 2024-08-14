package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateInterval;

record MedlemskapsvilkårRegelGrunnlag(LocalDateInterval vurderingsperiodeBosatt,
                                      LocalDateInterval vurderingsperiodeLovligOpphold, // To forskjellige grunnlag eller to vurderingsperioder på ett stort grunnlag
                                      Set<LocalDateInterval> registrertMedlemskapPerioder,
                                      Personopplysninger personopplysninger,
                                      Søknad søknad) {

    record Søknad(Set<LocalDateInterval> utenlandsopphold) {
    }

    record Personopplysninger(Set<RegionPeriode> regioner, Set<LocalDateInterval> oppholdstillatelser, Set<PersonstatusPeriode> personstatus, Set<Adresse> adresser) {

        record RegionPeriode(LocalDateInterval periode, Region region) {
        }

        enum Region {
            NORDEN(1),
            EØS(2),
            TREDJELAND(3);

            private final int prioritet;

            Region(int prioritet) {

                this.prioritet = prioritet;
            }


            int getPrioritet() {
                return prioritet;
            }
        }

        record PersonstatusPeriode(LocalDateInterval interval, Type type) {
            enum Type {
                D_NUMMER,
                BOSATT,
                DØD,
                FORSVUNNET,
                FØDSELREGISTRERT,
                UREGISTRERT,
                UTGÅTT_ANNULLERT,
                UTGÅTT,
                UTVANDRET
            }
        }
    }

    record Adresse(LocalDateInterval periode, Type type, boolean erUtenlandsk) {

        enum Type {
            BOSTED, ANNEN
        }
    }
}
