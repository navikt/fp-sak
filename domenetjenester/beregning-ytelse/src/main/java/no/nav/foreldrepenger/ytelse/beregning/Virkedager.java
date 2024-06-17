package no.nav.foreldrepenger.ytelse.beregning;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class Virkedager {

    private Virkedager() {
    }

    public static int beregnAntallVirkedager(LocalDate fom, LocalDate tom) {
        Objects.requireNonNull(fom);
        Objects.requireNonNull(tom);
        if (fom.isAfter(tom)) {
            throw new IllegalArgumentException("Utviklerfeil: fom " + fom + " kan ikke være før tom " + tom);
        } else {
            return beregnVirkedager(fom, tom);
        }
    }

    private static int beregnVirkedager(LocalDate fom, LocalDate tom) {
        try {
            var padBefore = fom.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
            var padAfter = DayOfWeek.SUNDAY.getValue() - tom.getDayOfWeek().getValue();
            var virkedagerPadded = Math.toIntExact(ChronoUnit.WEEKS.between(fom.minusDays(padBefore), tom.plusDays(padAfter).plusDays(1L)) * 5L);
            var virkedagerPadding = Math.min(padBefore, 5) + Math.max(padAfter - 2, 0);
            return virkedagerPadded - virkedagerPadding;
        } catch (ArithmeticException var6) {
            throw new UnsupportedOperationException("Perioden er for lang til å beregne virkedager.", var6);
        }
    }

}
