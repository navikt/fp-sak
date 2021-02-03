package no.nav.foreldrepenger.økonomistøtte.dagytelse.opphør;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;

public class FinnOppdragslinje150FomSisteOpphør {

    private FinnOppdragslinje150FomSisteOpphør() {
    }

    public static List<Oppdragslinje150> finnOppdragslinje150FomSisteOpphør(List<Oppdragslinje150> tidligereOpp150MedSammeKlassekodeListe) {
        Long sisteDelytelseIdForOpphør = finnDelytelseIdForSisteOpphør(tidligereOpp150MedSammeKlassekodeListe);
        List<Oppdragslinje150> fomSisteOpphørListe = finnOppdragslinje150FomSisteOpphør(sisteDelytelseIdForOpphør, tidligereOpp150MedSammeKlassekodeListe);

        return fomSisteOpphørListe.stream()
            .sorted(Comparator.comparing(Oppdragslinje150::getDelytelseId).thenComparing(Oppdragslinje150::getKodeStatusLinje, Comparator.nullsFirst(Comparator.naturalOrder())))
            .collect(Collectors.toList());
    }

    private static List<Oppdragslinje150> finnOppdragslinje150FomSisteOpphør(Long sisteDelytelseIdForOpphør, List<Oppdragslinje150> tidligereOpp150MedSammeKlassekodeListe) {
        List<Oppdragslinje150> fomSisteOpphørListe = getOppdragslinje150FomSisteOpphør(tidligereOpp150MedSammeKlassekodeListe, sisteDelytelseIdForOpphør);
        List<Oppdragslinje150> opp150ForOpphørListe = finnOpphørMedSammeDelytelseId(sisteDelytelseIdForOpphør, fomSisteOpphørListe);

        if (opp150ForOpphørListe.size() > 1) {
            LocalDate vedtakIdForSisteOpph = finnVedtakIdForSisteOpph(opp150ForOpphørListe);
            fomSisteOpphørListe.removeIf(oppdr150 -> LocalDate.parse(oppdr150.getVedtakId()).isBefore(vedtakIdForSisteOpph)
                && oppdr150.gjelderOpphør());
            return fomSisteOpphørListe;
        }
        return fomSisteOpphørListe;
    }

    private static List<Oppdragslinje150> finnOpphørMedSammeDelytelseId(Long sisteDelytelseIdForOpphør, List<Oppdragslinje150> fomSisteOpphørListe) {
        return fomSisteOpphørListe.stream()
            .filter(Oppdragslinje150::gjelderOpphør)
            .filter(opp150 -> opp150.getDelytelseId().equals(sisteDelytelseIdForOpphør))
            .collect(Collectors.toList());
    }

    private static LocalDate finnVedtakIdForSisteOpph(List<Oppdragslinje150> opp150ForOpphørListe) {
        return opp150ForOpphørListe.stream()
            .map(opp150 -> LocalDate.parse(opp150.getVedtakId()))
            .max(Comparator.comparing(Function.identity()))
            .orElseThrow(() -> new IllegalStateException("Oppdragslinje150 mangler vedtakId"));
    }

    private static List<Oppdragslinje150> getOppdragslinje150FomSisteOpphør(List<Oppdragslinje150> tidligereOpp150MedSammeKlassekodeListe, Long sisteDelytelseIdForOpphør) {
        List<Oppdragslinje150> fomSisteOpphørListe = tidligereOpp150MedSammeKlassekodeListe
            .stream()
            .filter(oppdr150 -> oppdr150.getDelytelseId().compareTo(sisteDelytelseIdForOpphør) > -1)
            .collect(Collectors.toList());
        fomSisteOpphørListe.removeIf(oppdr150 -> !oppdr150.gjelderOpphør() && oppdr150.getDelytelseId().equals(sisteDelytelseIdForOpphør));

        return fomSisteOpphørListe;
    }

    private static Long finnDelytelseIdForSisteOpphør(List<Oppdragslinje150> tidligereOpp150MedSammeKlassekodeListe) {
        return tidligereOpp150MedSammeKlassekodeListe.stream()
            .filter(Oppdragslinje150::gjelderOpphør)
            .map(Oppdragslinje150::getDelytelseId)
            .max(Comparator.comparing(Function.identity()))
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Mangler delytelseId"));
    }
}
