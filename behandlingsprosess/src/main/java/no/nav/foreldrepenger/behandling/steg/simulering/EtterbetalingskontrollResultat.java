package no.nav.foreldrepenger.behandling.steg.simulering;

import java.math.BigDecimal;

public record EtterbetalingskontrollResultat(BigDecimal etterbetalingssum, boolean overstigerGrense) {
    @Override
    public String toString() {
        return "EtterbetalingsKontroll{" + "etterbetalingssum=" + etterbetalingssum + ", overstigerGrense=" + overstigerGrense + '}';
    }
}
