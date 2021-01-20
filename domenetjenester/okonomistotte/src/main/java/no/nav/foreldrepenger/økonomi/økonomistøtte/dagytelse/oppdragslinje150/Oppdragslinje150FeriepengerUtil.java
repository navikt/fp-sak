package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdragslinje150;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.økonomi.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjenteFeriepenger;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjenteFeriepengerPrÅr;

public class Oppdragslinje150FeriepengerUtil {

    private Oppdragslinje150FeriepengerUtil() {
    }

    public static List<TilkjenteFeriepengerPrÅr> opprettOpp150FeriepengerListe(Oppdragsmottaker mottaker,
                                                                               TilkjenteFeriepenger tilkjenteFeriepenger) {
        if (mottaker.erBruker()) {
            return tilkjenteFeriepenger.getTilkjenteFeriepengerPrÅrList()
                .stream()
                .filter(TilkjenteFeriepengerPrÅr::skalTilBrukerEllerPrivatperson)
                .collect(Collectors.toList());
        } else {
            return tilkjenteFeriepenger.getTilkjenteFeriepengerPrÅrList()
                .stream()
                .filter(feriepengerPrÅr -> !feriepengerPrÅr.skalTilBrukerEllerPrivatperson()
                    && feriepengerPrÅr.getArbeidsforholdOrgnr().equals(mottaker.getOrgnr()))
                .collect(Collectors.toList());
        }
    }

    public static Set<LocalDate> getOpptjeningsdato(List<TilkjenteFeriepengerPrÅr> tilkjenteFeriepengerPrÅrList) {
        return tilkjenteFeriepengerPrÅrList.stream()
            .map(TilkjenteFeriepengerPrÅr::getOpptjeningÅr)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    public static List<Oppdragslinje150> finnOpp150FeriepengerMedMaxDelytelseIdForGittÅret(List<Oppdragslinje150> tidligereOpp150FeriepengerListe,
                                                                                           int feriepengeår) {
        NavigableMap<Long, List<Oppdragslinje150>> opp150PrDelytelseIdMap = tidligereOpp150FeriepengerListe.stream()
            .filter(oppdragslinje150 -> oppdragslinje150.getDatoVedtakFom().getYear() == feriepengeår)
            .sorted(Comparator.comparing(Oppdragslinje150::getDelytelseId).thenComparing(Oppdragslinje150::getKodeStatusLinje, Comparator.nullsFirst(Comparator.naturalOrder())))
            .collect(Collectors.groupingBy(Oppdragslinje150::getDelytelseId, TreeMap::new, Collectors.toList()));

        return opp150PrDelytelseIdMap.isEmpty() ? Collections.emptyList() : opp150PrDelytelseIdMap.lastEntry().getValue();
    }

    public static Map<Integer, List<Oppdragslinje150>> finnSisteOpp150ForFeriepenger(List<Oppdragslinje150> tidligereOpp150FeriepengerListe) {

        List<Oppdragslinje150> sisteOpp150ForFeriepengerList = new ArrayList<>();
        List<Oppdragslinje150> alleTidligereOpp150ForFeriepengerList = Oppdragslinje150Util.getOpp150ForFeriepengerMedKlassekode(tidligereOpp150FeriepengerListe);

        Map<Integer, List<Oppdragslinje150>> opp150PrFeriepengeårMap = alleTidligereOpp150ForFeriepengerList.stream()
            .collect(Collectors.groupingBy(opp150 -> opp150.getDatoVedtakFom().getYear()));

        for (Map.Entry<Integer, List<Oppdragslinje150>> entry : opp150PrFeriepengeårMap.entrySet()) {
            entry.getValue().stream()
                .max(Comparator.comparing(Oppdragslinje150::getDelytelseId).thenComparing(Oppdragslinje150::getKodeStatusLinje, Comparator.nullsFirst(Comparator.naturalOrder())))
                .ifPresent(sisteOpp150ForFeriepengerList::add);
        }
        return sisteOpp150ForFeriepengerList.stream()
            .collect(Collectors.groupingBy(opp150 -> opp150.getDatoVedtakFom().getYear()));
    }

    public static boolean finnesNyFeriepengeårIBehandling(List<Oppdragslinje150> tidligereOpp150FeriepengerListe, List<TilkjenteFeriepengerPrÅr> tilkjenteFeriepengerPrÅrList) {
        Set<Integer> feriepengeårListe = getFeriepengeår(tilkjenteFeriepengerPrÅrList);
        for (int feriepengeår : feriepengeårListe) {
            List<Oppdragslinje150> opp150MedMaxDelytelseIdListe = finnOpp150FeriepengerMedMaxDelytelseIdForGittÅret(tidligereOpp150FeriepengerListe, feriepengeår);
            boolean finnesBeregningForDetteÅretIForrige = opp150MedMaxDelytelseIdListe.size() == 1;
            if (finnesBeregningForDetteÅretIForrige) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static Set<Integer> getFeriepengeår(List<TilkjenteFeriepengerPrÅr> tilkjenteFeriepengerPrÅrList) {
        return tilkjenteFeriepengerPrÅrList.stream()
            .map(TilkjenteFeriepengerPrÅr::getOpptjeningÅr)
            .map(LocalDate::getYear)
            .collect(Collectors.toCollection(TreeSet::new));
    }
}

