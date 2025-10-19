package no.nav.foreldrepenger.behandlingslager.aktør;

import static java.util.Objects.requireNonNull;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public record PersoninfoVisning(AktørId aktørId,
                                PersonIdent personIdent,
                                String navn,
                                Diskresjonskode diskresjonskode) {

    public PersoninfoVisning {
        requireNonNull(aktørId, "Navbruker må ha aktørId");
        requireNonNull(personIdent, "Navbruker må ha fødselsnummer");
        requireNonNull(navn, "Navbruker må ha navn");
        requireNonNull(diskresjonskode, "Navbruker må ha diskresjonskode");
    }

}
