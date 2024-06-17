package no.nav.foreldrepenger.behandlingslager.aktør;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class PersoninfoArbeidsgiver {

    private AktørId aktørId;
    private String navn;
    private PersonIdent personIdent;
    private LocalDate fødselsdato;

    private PersoninfoArbeidsgiver() {
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public PersonIdent getPersonIdent() {
        return personIdent;
    }

    public String getNavn() {
        return navn;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (PersoninfoArbeidsgiver) o;
        return aktørId.equals(that.aktørId) && Objects.equals(navn, that.navn) && Objects.equals(personIdent, that.personIdent) && Objects.equals(
            fødselsdato, that.fødselsdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<aktørId=" + aktørId + ">";
    }

    public static class Builder {
        private PersoninfoArbeidsgiver personinfoMal;

        public Builder() {
            personinfoMal = new PersoninfoArbeidsgiver();
        }

        public Builder medAktørId(AktørId aktørId) {
            personinfoMal.aktørId = aktørId;
            return this;
        }

        public Builder medNavn(String navn) {
            personinfoMal.navn = navn;
            return this;
        }

        public Builder medPersonIdent(PersonIdent fnr) {
            personinfoMal.personIdent = fnr;
            return this;
        }

        public Builder medFødselsdato(LocalDate fødselsdato) {
            personinfoMal.fødselsdato = fødselsdato;
            return this;
        }

        public PersoninfoArbeidsgiver build() {
            requireNonNull(personinfoMal.aktørId, "Navbruker må ha aktørId");
            requireNonNull(personinfoMal.personIdent, "Navbruker må ha fødselsnummer");
            requireNonNull(personinfoMal.navn, "Navbruker må ha navn");
            requireNonNull(personinfoMal.fødselsdato, "Navbruker må ha fødselsdato");
            return personinfoMal;
        }

    }

}
