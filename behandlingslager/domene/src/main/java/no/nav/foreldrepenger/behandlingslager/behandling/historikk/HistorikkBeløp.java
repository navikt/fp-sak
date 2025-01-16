package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public record HistorikkBeløp(BigDecimal beløp) {

    public HistorikkBeløp {
        if (beløp == null) {
            throw new IllegalArgumentException("Beløp cannot be null");
        }
    }

    public static HistorikkBeløp ofNullable(BigDecimal beløp) {
        if (beløp == null) {
            return null;
        }
        return new HistorikkBeløp(beløp);
    }

    public static HistorikkBeløp ofNullable(Integer beløp) {
        if (beløp == null) {
            return null;
        }
        return new HistorikkBeløp(BigDecimal.valueOf(beløp));
    }

    public static HistorikkBeløp ofNullable(Long beløp) {
        if (beløp == null) {
            return null;
        }
        return new HistorikkBeløp(BigDecimal.valueOf(beløp));
    }

    public static HistorikkBeløp of(BigDecimal beløp) {
        return new HistorikkBeløp(beløp);
    }

    public static HistorikkBeløp of(Integer beløp) {
        return new HistorikkBeløp(BigDecimal.valueOf(beløp));
    }

    public static HistorikkBeløp of(Long beløp) {
        return new HistorikkBeløp(BigDecimal.valueOf(beløp));
    }

    @Override
    public String toString() {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("nb-NO")).format(beløp).concat(" kr");
    }
}
