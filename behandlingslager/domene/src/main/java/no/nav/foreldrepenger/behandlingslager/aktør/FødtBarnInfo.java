package no.nav.foreldrepenger.behandlingslager.aktør;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public record FødtBarnInfo(PersonIdent ident, LocalDate fødselsdato, LocalDate dødsdato, RelasjonsRolleType forelderRolle) {

    public Optional<LocalDate> getDødsdato() {
        return Optional.ofNullable(dødsdato);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FødtBarnInfo that && Objects.equals(ident, that.ident)
            && Objects.equals(dødsdato, that.dødsdato) && Objects.equals(fødselsdato, that.fødselsdato);
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
        private RelasjonsRolleType forelderRolle = RelasjonsRolleType.UDEFINERT;

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

        public Builder medForelderRolle(RelasjonsRolleType forelderRolle) {
            this.forelderRolle = forelderRolle;
            return this;
        }

        public FødtBarnInfo build() {
            // Vurder sjekking ident != null || dødsdato != null
            return new FødtBarnInfo(ident, fødselsdato, dødsdato, forelderRolle);
        }
    }
}
