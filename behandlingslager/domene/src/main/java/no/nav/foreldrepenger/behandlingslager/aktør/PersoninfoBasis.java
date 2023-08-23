package no.nav.foreldrepenger.behandlingslager.aktør;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

import java.time.LocalDate;

import static java.util.Objects.requireNonNull;

public record PersoninfoBasis(AktørId aktørId,
                              PersonIdent personIdent,
                              String navn,
                              LocalDate fødselsdato,
                              LocalDate dødsdato,
                              NavBrukerKjønn kjønn,
                              String diskresjonskode) {

    public PersoninfoBasis {
        requireNonNull(aktørId, "Navbruker må ha aktørId");
        requireNonNull(personIdent, "Navbruker må ha fødselsnummer");
        requireNonNull(navn, "Navbruker må ha navn");
        requireNonNull(kjønn, "Navbruker må ha kjønn");
    }

}
