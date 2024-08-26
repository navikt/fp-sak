package no.nav.foreldrepenger.domene.aksjonspunkt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

public class BeløpEndring {

    private final BigDecimal fraBeløp;
    private final BigDecimal tilBeløp;

    public BeløpEndring(BigDecimal fraBeløp, BigDecimal tilBeløp) {
        this.fraBeløp = fraBeløp;
        this.tilBeløp = tilBeløp;
    }

    public Optional<BigDecimal> getFraBeløp() {
        return Optional.ofNullable(fraBeløp).map(i -> i.setScale(0, RoundingMode.HALF_UP));
    }

    public BigDecimal getFraMånedsbeløp() {
        return fraBeløp == null ? null : fraBeløp.divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP).setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal getTilBeløp() {
        return tilBeløp == null ? null : tilBeløp.setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal getTilMånedsbeløp() {
        return tilBeløp == null ? null : tilBeløp.divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeløpEndring that = (BeløpEndring) o;
        return Objects.equals(fraBeløp, that.fraBeløp) && Objects.equals(tilBeløp, that.tilBeløp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fraBeløp, tilBeløp);
    }
}
