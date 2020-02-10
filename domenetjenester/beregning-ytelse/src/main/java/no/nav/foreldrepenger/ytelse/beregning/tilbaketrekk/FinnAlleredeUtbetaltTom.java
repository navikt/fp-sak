package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

class FinnAlleredeUtbetaltTom {
    private FinnAlleredeUtbetaltTom() {
        // skjul public constructor
    }

    static LocalDate finn(LocalDate idag) {
        if (idag.getDayOfMonth() > 15) {
            return idag.with(TemporalAdjusters.lastDayOfMonth());
        } else {
            return idag.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        }
    }
}
