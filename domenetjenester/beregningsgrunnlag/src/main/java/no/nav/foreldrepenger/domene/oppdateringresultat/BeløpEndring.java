package no.nav.foreldrepenger.domene.oppdateringresultat;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        return tilBeløp.setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal getTilMånedsbeløp() {
        return tilBeløp == null ? null : tilBeløp.divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP).setScale(0, RoundingMode.HALF_UP);
    }

}
