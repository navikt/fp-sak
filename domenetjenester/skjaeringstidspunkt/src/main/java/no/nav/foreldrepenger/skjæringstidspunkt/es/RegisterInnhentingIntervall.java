package no.nav.foreldrepenger.skjæringstidspunkt.es;

import java.time.LocalDate;
import java.time.Period;

public class RegisterInnhentingIntervall {

    /**
     * Maks avvik før/etter STP for registerinnhenting før justering av perioden
     */
    private static final Period GRENSEVERDI_FØR = Period.ofMonths(9);
    private static final Period GRENSEVERDI_ETTER = Period.ofMonths(6);

    private RegisterInnhentingIntervall() {
    }

    static boolean erEndringIPerioden(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt) {
        if (bekreftetSkjæringstidspunkt == null) {
            return false;
        }
        return vurderEndringFør(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt)
            || vurderEndringEtter(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt);
    }

    private static boolean vurderEndringEtter(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt) {
        var avstand = Period.between(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt);
        return !avstand.isNegative() && størreEnn(avstand, RegisterInnhentingIntervall.GRENSEVERDI_ETTER);
    }

    private static boolean vurderEndringFør(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt) {
        var avstand = Period.between(bekreftetSkjæringstidspunkt, oppgittSkjæringstidspunkt);
        return !avstand.isNegative() && størreEnn(avstand, RegisterInnhentingIntervall.GRENSEVERDI_FØR);
    }

    private static boolean størreEnn(Period period, Period sammenligning) {
        return tilDager(period) > tilDager(sammenligning);
    }

    private static int tilDager(Period period) {
        return period.getDays() + period.getMonths() * 30 + period.getYears() * 12 * 30;
    }
}
