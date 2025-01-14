package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public record HistorikkBelop(BigDecimal belop) {

    public static HistorikkBelop ofNullable(BigDecimal belop) {
        if (belop == null) {
            return null;
        }
        return new HistorikkBelop(belop);
    }

    public static HistorikkBelop ofNullable(Integer belop) {
        if (belop == null) {
            return null;
        }
        return new HistorikkBelop(BigDecimal.valueOf(belop));
    }

    @Override
    public String toString() {
        return NumberFormat.getIntegerInstance(new Locale("nb", "NO")).format(belop).concat(" kr");
    }
}
