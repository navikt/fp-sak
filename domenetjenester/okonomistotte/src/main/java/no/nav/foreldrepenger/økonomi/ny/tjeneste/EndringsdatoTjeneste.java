package no.nav.foreldrepenger.økonomi.ny.tjeneste;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import no.nav.foreldrepenger.økonomi.ny.domene.Periode;
import no.nav.foreldrepenger.økonomi.ny.domene.SatsType;
import no.nav.foreldrepenger.økonomi.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomi.ny.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomi.ny.domene.YtelseVerdi;

public class EndringsdatoTjeneste {
    public static LocalDate finnEndringsdato(Ytelse y1, Ytelse y2) {
        return finnEndringsdato(y1, y2, Function.identity());
    }

    public static LocalDate finnEndringsdatoForEndringSats(Ytelse y1, Ytelse y2) {
        return finnEndringsdato(y1, y2, YtelseVerdi::getSats);
    }

    public static LocalDate finnEndringsdatoForEndringUtbetalingsgrad(Ytelse y1, Ytelse y2) {
        return finnEndringsdato(y1, y2, YtelseVerdi::getUtbetalingsgrad);
    }

    private static <T> LocalDate finnEndringsdato(Ytelse y1, Ytelse y2, Function<YtelseVerdi, T> valueFunction) {
        SortedSet<LocalDate> knekkpunkter = finnKnekkpunkter(y1, y2);
        for (LocalDate knekkpunkt : knekkpunkter) {
            YtelseVerdi v1 = filtrer(y1.finnVerdiFor(knekkpunkt), knekkpunkt);
            YtelseVerdi v2 = filtrer(y2.finnVerdiFor(knekkpunkt), knekkpunkt);
            T verdi1 = v1 != null ? valueFunction.apply(v1) : null;
            T verdi2 = v2 != null ? valueFunction.apply(v2) : null;

            if (!Objects.equals(verdi1, verdi2)) {
                return knekkpunkt;
            }
        }
        return null;
    }

    private static YtelseVerdi filtrer(YtelseVerdi verdi, LocalDate dato) {
        if (verdi == null || verdi.getSats().getSatsType() == SatsType.DAG && (dato.getDayOfWeek() == DayOfWeek.SUNDAY || dato.getDayOfWeek() == DayOfWeek.SATURDAY)) {
            return null;
        }
        return verdi;
    }

    private static SortedSet<LocalDate> finnKnekkpunkter(Ytelse y1, Ytelse y2) {
        Objects.requireNonNull(y1);
        Objects.requireNonNull(y2);
        SortedSet<LocalDate> knekkpunkt = new TreeSet<>();
        knekkpunkt.addAll(finnKnekkpunkter(y1));
        knekkpunkt.addAll(finnKnekkpunkter(y2));
        return knekkpunkt;
    }


    private static Collection<LocalDate> finnKnekkpunkter(Ytelse ytelse) {
        Set<LocalDate> knekkpunkt = new TreeSet<>();
        for (YtelsePeriode ytelsePeriode : ytelse.getPerioder()) {
            knekkpunkt.addAll(lagKnekkpunkterFraPeriode(ytelsePeriode.getPeriode()));
        }
        return knekkpunkt;
    }

    private static List<LocalDate> lagKnekkpunkterFraPeriode(Periode periode) {
        List<LocalDate> knekkpunkter = new ArrayList<>();
        LocalDate fom = periode.getFom();
        LocalDate tom = periode.getTom();
        LocalDate dagenEtter = tom.plusDays(1);

        knekkpunkter.add(fom);
        knekkpunkter.add(dagenEtter);

        if (fom.getDayOfWeek() == DayOfWeek.SATURDAY) {
            knekkpunkter.add(fom.plusDays(2));
        } else if (fom.getDayOfWeek() == DayOfWeek.SUNDAY) {
            knekkpunkter.add(fom.plusDays(1));
        }
        if (dagenEtter.getDayOfWeek() == DayOfWeek.SATURDAY) {
            knekkpunkter.add(dagenEtter.plusDays(2));
        } else if (dagenEtter.getDayOfWeek() == DayOfWeek.SUNDAY) {
            knekkpunkter.add(dagenEtter.plusDays(1));
        }
        return knekkpunkter;
    }
}
