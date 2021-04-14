package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;

class FinnAlleredeUtbetaltTom {
    private FinnAlleredeUtbetaltTom() {
        // skjul public constructor
    }

    static LocalDate finn(LocalDate idag) {
        var utbetalingsdagIMåned = finnUtbetalingsdagForMåned(idag.getMonth());
        if (idag.getDayOfMonth() > utbetalingsdagIMåned) {
            return idag.with(TemporalAdjusters.lastDayOfMonth());
        }
        return idag.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
    }

    private static int finnUtbetalingsdagForMåned(Month month) {
        // Desember utbetaling er alltid tidligere enn andre måneder, spesialbehandles.
        if (month == Month.DECEMBER) {
            return 7;
        }
        return 18;
    }

}
