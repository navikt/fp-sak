package no.nav.foreldrepenger.økonomi.ny.tjeneste;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import no.nav.foreldrepenger.økonomi.ny.domene.Periode;
import no.nav.foreldrepenger.økonomi.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomi.ny.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomi.ny.domene.YtelseVerdi;

public class EndringsdatoTjeneste {
    public static LocalDate finnEndringsdato(Ytelse y1, Ytelse y2) {
        SortedSet<LocalDate> knekkpunkter = finnKnekkpunkter(y1, y2);
        for (LocalDate knekkpunkt : knekkpunkter) {
            YtelseVerdi v1 = y1.finnVerdiFor(knekkpunkt);
            YtelseVerdi v2 = y2.finnVerdiFor(knekkpunkt);
            if (!Objects.equals(v1, v2)) {
                return knekkpunkt;
            }
        }
        return null;
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
            Periode p = ytelsePeriode.getPeriode();
            knekkpunkt.add(p.getFom());
            knekkpunkt.add(p.getTom().plusDays(1));
        }
        return knekkpunkt;
    }
}
