package no.nav.foreldrepenger.behandlingslager.aktør;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public record PersoninfoBasis(AktørId aktørId, PersonIdent personIdent, String navn, LocalDate fødselsdato, LocalDate dødsdato, NavBrukerKjønn kjønn,
                              String diskresjonskode) {

    public PersoninfoBasis {
        requireNonNull(aktørId, "Navbruker må ha aktørId");
        requireNonNull(personIdent, "Navbruker må ha fødselsnummer");
        requireNonNull(navn, "Navbruker må ha navn");
        requireNonNull(kjønn, "Navbruker må ha kjønn");
    }

}
