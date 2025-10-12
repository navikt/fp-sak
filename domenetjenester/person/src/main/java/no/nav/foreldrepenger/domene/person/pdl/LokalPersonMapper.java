package no.nav.foreldrepenger.domene.person.pdl;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.pdl.KjoennType;
import no.nav.pdl.Person;
import no.nav.vedtak.felles.integrasjon.person.PersonMappers;

public class LokalPersonMapper {

    private LokalPersonMapper() {
    }

    public static NavBrukerKjønn mapKjønn(Person person) {
        return mapKjønn(PersonMappers.mapKjønn(person));
    }

    public static NavBrukerKjønn mapKjønn(KjoennType kjoennType) {
        return switch (kjoennType) {
            case KVINNE -> NavBrukerKjønn.KVINNE;
            case MANN -> NavBrukerKjønn.MANN;
            case UKJENT -> NavBrukerKjønn.UDEFINERT;
        };
    }

    public static String mapNavn(Person person) {
        return PersonMappers.mapNavn(person).orElse("Ukjent Navn");
    }

    public static LocalDate mapFødselsdatoEllerDefault(Person person, LocalDate tid) {
        return PersonMappers.mapFødselsdato(person).orElse(tid);
    }

    public static LocalDate mapDødsdato(Person person) {
        return PersonMappers.mapDødsdato(person).orElse(null);
    }

}
