package no.nav.foreldrepenger.behandlingslager.aktør;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.domene.typer.PersonIdent;

public record FødtBarnInfo(PersonIdent ident, LocalDate fødselsdato, LocalDate dødsdato) {

    public Optional<LocalDate> getDødsdato() {
        return Optional.ofNullable(dødsdato);
    }

    @Override
    public String toString() {
        return "FødtBarnInfo{" + "fødselsdato=" + fødselsdato + ", dødsdato=" + dødsdato + '}';
    }

    public static class Builder {
        private PersonIdent ident;
        private LocalDate fødselsdato;
        private LocalDate dødsdato;

        public Builder medIdent(PersonIdent ident) {
            this.ident = ident;
            return this;
        }

        public Builder medFødselsdato(LocalDate fødselsdato) {
            this.fødselsdato = fødselsdato;
            return this;
        }

        public Builder medDødsdato(LocalDate dødsdato) {
            this.dødsdato = dødsdato;
            return this;
        }

        public FødtBarnInfo build() {
            // Vurder sjekking ident != null || dødsdato != null
            return new FødtBarnInfo(ident, fødselsdato, dødsdato);
        }
    }
}
