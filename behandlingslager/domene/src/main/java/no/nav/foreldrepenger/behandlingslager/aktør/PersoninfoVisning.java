package no.nav.foreldrepenger.behandlingslager.aktør;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

import static java.util.Objects.requireNonNull;

public record PersoninfoVisning(AktørId aktørId,
                                PersonIdent personIdent,
                                String navn,
                                Diskresjonskode diskresjonskode) {

    public PersoninfoVisning {
        requireNonNull(aktørId, "Navbruker må ha aktørId");
        requireNonNull(personIdent, "Navbruker må ha fødselsnummer");
        requireNonNull(navn, "Navbruker må ha navn");
    }

}
