package no.nav.foreldrepenger.behandlingslager.aktør;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.domene.typer.PersonIdent;

public record FødtBarnInfo(PersonIdent ident, LocalDate fødselsdato, LocalDate dødsdato) {
  
    public FødtBarnInfo(PersonIdent ident, LocalDate fødselsdato) {
        this(ident, fødselsdato, null);
    }

    public Optional<LocalDate> getDødsdato() {
        return Optional.ofNullable(dødsdato());
    }
}
