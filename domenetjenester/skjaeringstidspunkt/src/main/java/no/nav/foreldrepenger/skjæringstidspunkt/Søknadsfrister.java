package no.nav.foreldrepenger.skjæringstidspunkt;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;

/*
 * Se Ftl 22-13 andre ledd om engangsutbetaling og tredje ledd om dagytelser.
 */
public final class Søknadsfrister {

    private static final Period FRIST_ENGANGS = Period.ofMonths(6);
    private static final Period FRIST_DAGYTELSE = Period.ofMonths(3);

    public static LocalDate søknadsfristEngangsbeløp(LocalDate rettUtløstDato) {
        return rettUtløstDato.plus(FRIST_ENGANGS);
    }

    public static LocalDate søknadsfristDagytelse(LocalDate periodeStart) {
        return periodeStart.plus(FRIST_DAGYTELSE).with(TemporalAdjusters.lastDayOfMonth());
    }

    public static LocalDate tidligsteDatoDagytelse(LocalDate søknadMottattDato) {
        return søknadMottattDato.minus(FRIST_DAGYTELSE).withDayOfMonth(1);
    }

}
