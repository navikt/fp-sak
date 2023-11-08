package no.nav.foreldrepenger.skjæringstidspunkt.es;

import java.time.LocalDate;
import java.time.Period;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
public class RegisterInnhentingIntervall {

    private Period grenseverdiFør;
    private Period grenseverdiEtter;

    RegisterInnhentingIntervall() {
        // CDI
    }

    /**
     * @param grenseverdiFørES- Maks avvik før STP for registerinnhenting før justering av perioden (Engangsstønad)
     * @param grenseverdiPeriodeEtter Maks avvik etter STP for registerinnhenting før justering av perioden (Engangsstønad)
     */
    @Inject
    public RegisterInnhentingIntervall(@KonfigVerdi(value="es.registerinnhenting.avvik.periode.før", defaultVerdi = "P9M") Period grenseverdiPeriodeFør,
                                       @KonfigVerdi(value="es.registerinnhenting.avvik.periode.etter", defaultVerdi = "P6M") Period grenseverdiPeriodeEtter) {
        this.grenseverdiFør = grenseverdiPeriodeFør;
        this.grenseverdiEtter = grenseverdiPeriodeEtter;
    }

    boolean erEndringIPerioden(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt) {
        if (bekreftetSkjæringstidspunkt == null) {
            return false;
        }
        return vurderEndringFør(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt, grenseverdiFør)
            || vurderEndringEtter(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt, grenseverdiEtter);
    }

    private boolean vurderEndringEtter(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt, Period grenseverdiEtter) {
        var avstand = Period.between(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt);
        return !avstand.isNegative() && størreEnn(avstand, grenseverdiEtter);
    }

    private boolean vurderEndringFør(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt, Period grenseverdiFør) {
        var avstand = Period.between(bekreftetSkjæringstidspunkt, oppgittSkjæringstidspunkt);
        return !avstand.isNegative() && størreEnn(avstand, grenseverdiFør);
    }

    private static boolean størreEnn(Period period, Period sammenligning) {
        return tilDager(period) > tilDager(sammenligning);
    }

    private static int tilDager(Period period) {
        return period.getDays() + period.getMonths() * 30 + period.getYears() * 12 * 30;
    }
}
