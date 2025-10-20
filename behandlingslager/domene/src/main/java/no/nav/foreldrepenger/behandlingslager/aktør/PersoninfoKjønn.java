package no.nav.foreldrepenger.behandlingslager.aktør;

import static java.util.Objects.requireNonNull;

import no.nav.foreldrepenger.domene.typer.AktørId;

public record PersoninfoKjønn(AktørId aktørId, NavBrukerKjønn kjønn) {

    public PersoninfoKjønn {
        requireNonNull(aktørId, "Navbruker må ha aktørId");
        requireNonNull(kjønn, "Navbruker må ha kjønn");
    }
}
