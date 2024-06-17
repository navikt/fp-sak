package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Objects;

public class Statsborgerskap {

    private String landkode;

    public Statsborgerskap(String landkode) {
        this.landkode = landkode;
    }

    public String getLandkode() {
        return landkode;
    }

    public void setLandkode(String landkode) {
        this.landkode = landkode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (Statsborgerskap) o;
        return Objects.equals(landkode, that.landkode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(landkode);
    }
}
