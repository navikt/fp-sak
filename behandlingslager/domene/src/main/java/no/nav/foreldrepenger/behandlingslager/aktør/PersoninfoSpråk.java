package no.nav.foreldrepenger.behandlingslager.aktør;

import static java.util.Objects.requireNonNull;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.typer.AktørId;

public record PersoninfoSpråk(AktørId aktørId, Språkkode foretrukketSpråk) {

    public PersoninfoSpråk {
        requireNonNull(aktørId, "Navbruker må ha aktørId");
    }

}
