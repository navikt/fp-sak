package no.nav.foreldrepenger.domene.uttak;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class PerioderUtenHelgUtil {

    private PerioderUtenHelgUtil() {
    }

    public static boolean datoerLikeNårHelgIgnoreres(LocalDate dato1, LocalDate dato2) {
        return justerFom(dato1).equals(justerFom(dato2));
    }

    private static LocalDate justerFom(LocalDate fom) {
        if (fom.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return fom.plusDays(2);
        } else if (fom.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return fom.plusDays(1);
        } else {
            return fom;
        }
    }
}
