package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public record HistorikkBelop(BigDecimal beløp) {
    public static HistorikkBelop valueOf(BigDecimal beløp) {
        return new HistorikkBelop(beløp);
    }

    public static HistorikkBelop valueOf(Integer beløp) {
        return new HistorikkBelop(BigDecimal.valueOf(beløp));
    }

    @Override
    public String toString() {
        var numberFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("nb-NO"));
        numberFormatter.setRoundingMode(RoundingMode.HALF_UP);
        numberFormatter.setMaximumFractionDigits(0);
        return numberFormatter.format(beløp);
    }
}
