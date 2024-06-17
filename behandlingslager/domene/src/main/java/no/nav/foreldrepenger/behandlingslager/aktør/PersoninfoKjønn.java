package no.nav.foreldrepenger.behandlingslager.aktør;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import no.nav.foreldrepenger.domene.typer.AktørId;

public class PersoninfoKjønn {

    private AktørId aktørId;
    private NavBrukerKjønn kjønn;

    private PersoninfoKjønn() {
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public NavBrukerKjønn getKjønn() {
        return kjønn;
    }

    public boolean erKvinne() {
        return kjønn.equals(NavBrukerKjønn.KVINNE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (PersoninfoKjønn) o;
        return aktørId.equals(that.aktørId) && kjønn == that.kjønn;
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
        private PersoninfoKjønn personinfoMal;

        public Builder() {
            personinfoMal = new PersoninfoKjønn();
        }

        public Builder medAktørId(AktørId aktørId) {
            personinfoMal.aktørId = aktørId;
            return this;
        }

        public Builder medNavBrukerKjønn(NavBrukerKjønn kjønn) {
            personinfoMal.kjønn = kjønn;
            return this;
        }

        public PersoninfoKjønn build() {
            requireNonNull(personinfoMal.aktørId, "Navbruker må ha aktørId");
            requireNonNull(personinfoMal.kjønn, "Navbruker må ha kjønn");
            return personinfoMal;
        }

    }

}
