package no.nav.foreldrepenger.domene.aksjonspunkt;

import java.util.Objects;
import java.util.Optional;

public class ToggleEndring {

    private final Boolean fraVerdi;
    private final Boolean tilVerdi;

    public ToggleEndring(Boolean fraVerdi, Boolean tilVerdi) {
        this.fraVerdi = fraVerdi;
        this.tilVerdi = tilVerdi;
    }

    public boolean erEndret() {
        return !tilVerdi.equals(fraVerdi);
    }

    public Boolean getFraVerdiEllerNull() {
        return fraVerdi;
    }

    public Optional<Boolean> getFraVerdi() {
        return Optional.ofNullable(fraVerdi);
    }

    public Boolean getTilVerdi() {
        return tilVerdi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToggleEndring that = (ToggleEndring) o;
        return Objects.equals(fraVerdi, that.fraVerdi) && Objects.equals(tilVerdi, that.tilVerdi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fraVerdi, tilVerdi);
    }
}
