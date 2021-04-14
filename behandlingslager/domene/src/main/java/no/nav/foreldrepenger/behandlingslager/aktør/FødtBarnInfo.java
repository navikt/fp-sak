package no.nav.foreldrepenger.behandlingslager.aktør;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class FødtBarnInfo {

    private PersonIdent ident;
    private LocalDate fødselsdato;
    private LocalDate dødsdato;

    private FødtBarnInfo(PersonIdent ident, LocalDate fødselsdato, LocalDate dødsdato) {
        this.ident = ident;
        this.fødselsdato = fødselsdato;
        this.dødsdato = dødsdato;
    }

    // OBS: vil være null ved dødfødsel
    public PersonIdent getIdent() {
        return ident;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public Optional<LocalDate> getDødsdato() {
        return Optional.ofNullable(dødsdato);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (FødtBarnInfo) o;
        return Objects.equals(ident, that.ident) &&
            Objects.equals(fødselsdato, that.fødselsdato) &&
            Objects.equals(dødsdato, that.dødsdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ident, fødselsdato, dødsdato);
    }

    @Override
    public String toString() {
        return "FødtBarnInfo{" +
            "fødselsdato=" + fødselsdato +
            ", dødsdato=" + dødsdato +
            '}';
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
            return new FødtBarnInfo(ident, fødselsdato, dødsdato);
        }
    }
}
