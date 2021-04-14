package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Objects;

public class OrganisasjonsEnhet {

    private String enhetId;
    private String enhetNavn;

    public OrganisasjonsEnhet(String enhetId, String enhetNavn) {
        this.enhetId = enhetId;
        this.enhetNavn = enhetNavn;
    }

    public String getEnhetId() { return enhetId; }

    public String getEnhetNavn(){ return enhetNavn; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (OrganisasjonsEnhet) o;
        return Objects.equals(enhetId, that.enhetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enhetId);
    }

    @Override
    public String toString() {
        return "OrganisasjonsEnhet{" +
            "enhetId='" + enhetId + '\'' +
            ", enhetNavn='" + enhetNavn + '\'' +
            '}';
    }
}
