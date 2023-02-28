package no.nav.foreldrepenger.domene.uttak;

import static java.time.temporal.TemporalAdjusters.next;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public final class PerioderUtenHelgUtil {

    private static final Set<DayOfWeek> WEEKEND = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private PerioderUtenHelgUtil() {
    }

    public static boolean datoerLikeNårHelgIgnoreres(LocalDate dato1, LocalDate dato2) {
        return justerFomMandag(dato1).equals(justerFomMandag(dato2));
    }

    public static LocalDate justerFomMandag(LocalDate fom) {
        return WEEKEND.contains(fom.getDayOfWeek()) ? fom.with(next(DayOfWeek.MONDAY)) : fom;
    }

    public static LocalDate justerTomFredag(LocalDate tom) {
        return WEEKEND.contains(tom.getDayOfWeek()) ? tom.with(DayOfWeek.FRIDAY) : tom;
    }
}
