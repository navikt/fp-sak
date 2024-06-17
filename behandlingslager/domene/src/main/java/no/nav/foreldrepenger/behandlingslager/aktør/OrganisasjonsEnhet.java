package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Objects;

public record OrganisasjonsEnhet(String enhetId, String enhetNavn) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (OrganisasjonsEnhet) o;
        return Objects.equals(enhetId(), that.enhetId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(enhetId());
    }

}
