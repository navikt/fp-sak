package no.nav.foreldrepenger.domene.aksjonspunkt;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record BeløpEndring(BigDecimal fraBeløp, BigDecimal tilBeløp) {

    public BigDecimal getFraMånedsbeløp() {
        return fraBeløp == null ? null : fraBeløp.divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP);
    }

    public BigDecimal getTilMånedsbeløp() {
        return tilBeløp == null ? null : tilBeløp.divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP);
    }
}

