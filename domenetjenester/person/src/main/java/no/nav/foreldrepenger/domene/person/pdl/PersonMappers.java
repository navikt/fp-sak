package no.nav.foreldrepenger.domene.person.pdl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.Folkeregisteridentifikator;
import no.nav.pdl.Kjoenn;
import no.nav.pdl.KjoennType;
import no.nav.pdl.Navn;
import no.nav.pdl.Person;
import no.nav.pdl.Personnavn;

public class PersonMappers {

    private static final Logger LOG = LoggerFactory.getLogger(PersonMappers.class);

    private PersonMappers() {
    }

    public static boolean harIdentifikator(Person person) {
        return harIdentifikator(person.getFolkeregisteridentifikator());
    }

    public static boolean harIdentifikator(Collection<Folkeregisteridentifikator> folkeregisteridentifikator) {
        return folkeregisteridentifikator.stream()
            .map(Folkeregisteridentifikator::getStatus)
            .anyMatch("I_BRUK"::equals);
    }

    public static NavBrukerKjønn mapKjønn(Person person) {
        return person.getKjoenn().stream()
            .map(Kjoenn::getKjoenn)
            .filter(Objects::nonNull)
            .map(PersonMappers::mapKjønn)
            .filter(k -> !NavBrukerKjønn.UDEFINERT.equals(k))
            .findFirst().orElse(NavBrukerKjønn.UDEFINERT);
    }

    public static NavBrukerKjønn mapKjønn(KjoennType kjønn) {
        if (KjoennType.MANN.equals(kjønn))
            return NavBrukerKjønn.MANN;
        return KjoennType.KVINNE.equals(kjønn) ? NavBrukerKjønn.KVINNE : NavBrukerKjønn.UDEFINERT;
    }

    public static String mapNavn(Person person, AktørId aktørId) {
        return person.getNavn().stream()
            .filter(Objects::nonNull)
            .map(PersonMappers::mapNavn)
            .findFirst().orElseGet(() -> {
                LOG.warn("PDL mangler navn for aktørId={}", aktørId);
                return "Navn Utilgjengelig";
            });
    }

    public static String mapNavn(Navn navn) {
        return navn.getFornavn() + leftPad(navn.getMellomnavn()) + leftPad(navn.getEtternavn());
    }

    public static String mapNavn(Personnavn navn) {
        return navn.getFornavn() + leftPad(navn.getMellomnavn()) + leftPad(navn.getEtternavn());
    }

    public static String leftPad(String navn) {
        return Optional.ofNullable(navn).map(n -> " " + navn).orElse("");
    }

    public static LocalDate mapFødselsdato(Person person) {
        return person.getFoedselsdato().stream()
            .filter(Objects::nonNull)
            .map(PersonMappers::mapFødselsdato)
            .filter(Objects::nonNull)
            .findFirst().orElse(null);
    }

    public static LocalDate mapFødselsdato(Foedselsdato fødselsdato) {
        return Optional.ofNullable(fødselsdato.getFoedselsdato()).map(PersonMappers::mapDato).orElse(null);
    }

    public static LocalDate mapDødsdato(Person person) {
        return person.getDoedsfall().stream()
            .filter(Objects::nonNull)
            .map(PersonMappers::mapDødsdato)
            .filter(Objects::nonNull)
            .findFirst().orElse(null);
    }

    public static LocalDate mapDødsdato(Doedsfall dødsfall) {
        return Optional.ofNullable(dødsfall.getDoedsdato()).map(PersonMappers::mapDato).orElse(null);
    }

    public static LocalDate mapDato(String dato) {
        return Optional.ofNullable(dato).map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
    }

}
