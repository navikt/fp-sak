package no.nav.foreldrepenger.domene.aksjonspunkt;

import java.math.BigDecimal;

public class RefusjonEndring {

    private final BigDecimal fraRefusjon;
    private final BigDecimal tilRefusjon;

    public RefusjonEndring(BigDecimal fraRefusjon, BigDecimal tilRefusjon) {
        this.fraRefusjon = fraRefusjon;
        this.tilRefusjon = tilRefusjon;
    }

    public BigDecimal getFraRefusjon() {
        return fraRefusjon;
    }

    public BigDecimal getTilRefusjon() {
        return tilRefusjon;
    }

}
