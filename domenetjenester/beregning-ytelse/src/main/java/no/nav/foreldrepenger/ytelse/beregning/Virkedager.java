package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class Virkedager {
    private static final int DAGER_PR_UKE = 7;
    private static final int VIRKEDAGER_PR_UKE = 5;
    private static final int HELGEDAGER_PR_UKE = 2;

    private Virkedager() {
    }

    public static int beregnAntallVirkedager(Periode periode) {
        Objects.requireNonNull(periode);
        return beregnAntallVirkedager(periode.getFom(), periode.getTom());
    }

    public static int beregnAntallVirkedagerEllerKunHelg(LocalDate fom, LocalDate tom) {
        Objects.requireNonNull(fom);
        Objects.requireNonNull(tom);
        if (fom.isAfter(tom)) {
            throw new IllegalArgumentException("Utviklerfeil: fom " + fom + " kan ikke være før tom " + tom);
        } else {
            int varighetDager = (int)Periode.of(fom, tom).getVarighetDager();
            return varighetDager <= 2 && erHelg(fom) && erHelg(tom) ? varighetDager : beregnVirkedager(fom, tom);
        }
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
            int padBefore = fom.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
            int padAfter = DayOfWeek.SUNDAY.getValue() - tom.getDayOfWeek().getValue();
            int virkedagerPadded = Math.toIntExact(
                ChronoUnit.WEEKS.between(fom.minusDays((long)padBefore), tom.plusDays((long)padAfter).plusDays(1L)) * 5L);
            int virkedagerPadding = Math.min(padBefore, 5) + Math.max(padAfter - 2, 0);
            return virkedagerPadded - virkedagerPadding;
        } catch (ArithmeticException var6) {
            throw new UnsupportedOperationException("Perioden er for lang til å beregne virkedager.", var6);
        }
    }

    public static LocalDate plusVirkedager(LocalDate fom, int virkedager) {
        int uker = virkedager / 5;
        int dager = virkedager % 5;

        LocalDate resultat;
        for(resultat = fom.plusWeeks((long)uker); dager > 0 || erHelg(resultat); resultat = resultat.plusDays(1L)) {
            if (!erHelg(resultat)) {
                --dager;
            }
        }

        return resultat;
    }

    private static boolean erHelg(LocalDate dato) {
        return dato.getDayOfWeek().equals(DayOfWeek.SATURDAY) || dato.getDayOfWeek().equals(DayOfWeek.SUNDAY);
    }
}
