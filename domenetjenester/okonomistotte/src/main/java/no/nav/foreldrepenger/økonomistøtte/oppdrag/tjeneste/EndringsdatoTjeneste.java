package no.nav.foreldrepenger.økonomistøtte.oppdrag.tjeneste;

import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.*;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.util.SetUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

public class EndringsdatoTjeneste {

    private final boolean ignorerDagsatsIHelg;

    public static EndringsdatoTjeneste ignorerDagsatsIHelg() {
        return new EndringsdatoTjeneste(true);
    }

    public static EndringsdatoTjeneste normal() {
        return new EndringsdatoTjeneste(false);
    }

    private EndringsdatoTjeneste(boolean ignorerDagsatsIHelg) {
        this.ignorerDagsatsIHelg = ignorerDagsatsIHelg;
    }

    public LocalDate finnEndringsdato(Ytelse y1, Ytelse y2) {
        return finnEndringsdato(y1, y2, Function.identity());
    }

    public LocalDate finnEndringsdatoForEndringSats(Ytelse y1, Ytelse y2) {
        return finnEndringsdato(y1, y2, YtelseVerdi::getSats);
    }

    public LocalDate finnEndringsdatoForEndringUtbetalingsgrad(Ytelse y1, Ytelse y2) {
        return finnEndringsdato(y1, y2, YtelseVerdi::getUtbetalingsgrad);
    }

    private <T> LocalDate finnEndringsdato(Ytelse y1, Ytelse y2, Function<YtelseVerdi, T> valueFunction) {
        var knekkpunkter = finnKnekkpunkter(y1, y2);
        for (var knekkpunkt : knekkpunkter) {
            var v1 = filtrer(y1.finnVerdiFor(knekkpunkt), knekkpunkt);
            var v2 = filtrer(y2.finnVerdiFor(knekkpunkt), knekkpunkt);
            var verdi1 = v1 != null ? valueFunction.apply(v1) : null;
            var verdi2 = v2 != null ? valueFunction.apply(v2) : null;

            if (!Objects.equals(verdi1, verdi2)) {
                return knekkpunkt;
            }
        }
        return null;
    }

    public LocalDate finnTidligsteEndringsdato(GruppertYtelse målbilde, OverordnetOppdragKjedeOversikt tidligereOppdrag) {
        var nøkler = SetUtil.sortertUnionOfKeys(målbilde.getYtelsePrNøkkel(), tidligereOppdrag.getKjeder());
        LocalDate tidligsteEndringsdato = null;
        for (var nøkkel : nøkler) {
            var ytelse = målbilde.getYtelsePrNøkkel().getOrDefault(nøkkel, Ytelse.EMPTY);
            var oppdragskjede = tidligereOppdrag.getKjeder().getOrDefault(nøkkel, OppdragKjede.EMPTY);
            var endringsdato = finnEndringsdato(ytelse, oppdragskjede.tilYtelse());
            if (endringsdato != null && (tidligsteEndringsdato == null || endringsdato.isBefore(tidligsteEndringsdato))) {
                tidligsteEndringsdato = endringsdato;
            }
        }
        return tidligsteEndringsdato;
    }

    private YtelseVerdi filtrer(YtelseVerdi verdi, LocalDate dato) {
        if (ignorerDagsatsIHelg && (verdi == null || verdi.getSats().getSatsType() == SatsType.DAG && (dato.getDayOfWeek() == DayOfWeek.SUNDAY || dato.getDayOfWeek() == DayOfWeek.SATURDAY))) {
            return null;
        }
        return verdi;
    }

    private SortedSet<LocalDate> finnKnekkpunkter(Ytelse y1, Ytelse y2) {
        Objects.requireNonNull(y1);
        Objects.requireNonNull(y2);
        SortedSet<LocalDate> knekkpunkt = new TreeSet<>();
        knekkpunkt.addAll(finnKnekkpunkter(y1));
        knekkpunkt.addAll(finnKnekkpunkter(y2));
        return knekkpunkt;
    }


    private Collection<LocalDate> finnKnekkpunkter(Ytelse ytelse) {
        Set<LocalDate> knekkpunkt = new TreeSet<>();
        for (var ytelsePeriode : ytelse.getPerioder()) {
            knekkpunkt.addAll(lagKnekkpunkterFraPeriode(ytelsePeriode.getPeriode()));
        }
        return knekkpunkt;
    }

    private List<LocalDate> lagKnekkpunkterFraPeriode(Periode periode) {
        List<LocalDate> knekkpunkter = new ArrayList<>();
        var fom = periode.getFom();
        var tom = periode.getTom();
        var dagenEtter = tom.plusDays(1);

        knekkpunkter.add(fom);
        knekkpunkter.add(dagenEtter);

        if (ignorerDagsatsIHelg) {
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
        }
        return knekkpunkter;
    }
}
