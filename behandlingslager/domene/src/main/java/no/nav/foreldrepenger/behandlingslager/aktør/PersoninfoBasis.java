package no.nav.foreldrepenger.behandlingslager.aktør;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class PersoninfoBasis {

    private AktørId aktørId;
    private String navn;
    private PersonIdent personIdent;
    private LocalDate fødselsdato;
    private LocalDate dødsdato;
    private PersonstatusType personstatus;
    private NavBrukerKjønn kjønn;
    private String diskresjonskode;

    private PersoninfoBasis() {
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

    public NavBrukerKjønn getKjønn() {
        return kjønn;
    }

    public PersonstatusType getPersonstatus() {
        return personstatus;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public boolean erKvinne() {
        return kjønn.equals(NavBrukerKjønn.KVINNE);
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public String getDiskresjonskode() {
        return diskresjonskode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersoninfoBasis that = (PersoninfoBasis) o;
        return aktørId.equals(that.aktørId) &&
            Objects.equals(navn, that.navn) &&
            Objects.equals(personIdent, that.personIdent) &&
            Objects.equals(fødselsdato, that.fødselsdato) &&
            Objects.equals(dødsdato, that.dødsdato) &&
            personstatus == that.personstatus &&
            kjønn == that.kjønn &&
            Objects.equals(diskresjonskode, that.diskresjonskode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<aktørId=" + aktørId + ">"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static class Builder {
        private PersoninfoBasis personinfoMal;

        public Builder() {
            personinfoMal = new PersoninfoBasis();
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

        public Builder medDødsdato(LocalDate dødsdato) {
            personinfoMal.dødsdato = dødsdato;
            return this;
        }

        public Builder medPersonstatusType(PersonstatusType personstatus) {
            personinfoMal.personstatus = personstatus;
            return this;
        }

        public Builder medNavBrukerKjønn(NavBrukerKjønn kjønn) {
            personinfoMal.kjønn = kjønn;
            return this;
        }

        public Builder medDiskresjonsKode(String diskresjonsKode) {
            personinfoMal.diskresjonskode = diskresjonsKode;
            return this;
        }

        public PersoninfoBasis build() {
            requireNonNull(personinfoMal.aktørId, "Navbruker må ha aktørId"); //$NON-NLS-1$
            requireNonNull(personinfoMal.personIdent, "Navbruker må ha fødselsnummer"); //$NON-NLS-1$
            requireNonNull(personinfoMal.navn, "Navbruker må ha navn"); //$NON-NLS-1$
            //requireNonNull(personinfoMal.fødselsdato, "Navbruker må ha fødselsdato"); //$NON-NLS-1$
            requireNonNull(personinfoMal.kjønn, "Navbruker må ha kjønn"); //$NON-NLS-1$
            return personinfoMal;
        }

    }

}
