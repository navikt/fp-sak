package no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.Oppdragsmottaker;

public class Oppdragslinje150Util {

    private static final String REFUNDERES_ID_PREFIX = "00";
    private static final int REFUNDERES_ID_LENGDE = 11;

    private Oppdragslinje150Util() {
    }

    public static List<Oppdragslinje150> getOppdragslinje150ForOpphør(List<Oppdragslinje150> oppdragslinjer) {
        return oppdragslinjer.stream()
            .filter(Oppdragslinje150::gjelderOpphør)
            .collect(Collectors.toList());
    }

    public static Oppdragslinje150 getOpp150MedMaxDelytelseId(List<Oppdragslinje150> oppdragslinjer) {
        return oppdragslinjer.stream()
            .max(Comparator.comparing(Oppdragslinje150::getDelytelseId).thenComparing(Oppdragslinje150::getKodeStatusLinje, Comparator.nullsFirst(Comparator.naturalOrder())))
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Mangler delytelseId"));
    }

    public static List<Oppdragslinje150> getOpp150ForFeriepengerMedKlassekode(List<Oppdragslinje150> oppdragslinjer) {
        return oppdragslinjer.stream()
            .filter(oppdragslinje150 -> ØkonomiKodeKlassifik.fraKode(oppdragslinje150.getKodeKlassifik()).gjelderFerie())
            .collect(Collectors.toList());
    }

    public static List<Oppdragslinje150> finnOppdragslinje150MedRefunderesId(Oppdragsmottaker mottaker, List<Oppdragslinje150> sisteLinjeKjedeForArbeidsgivereListe) {
        return sisteLinjeKjedeForArbeidsgivereListe.stream()
            .filter(opp150 -> opp150.getRefusjonsinfo156().getRefunderesId().equals(Oppdragslinje150Util.endreTilElleveSiffer(mottaker.getId())))
            .collect(Collectors.toList());
    }

    public static String endreTilElleveSiffer(String id) {
        if (id.length() == REFUNDERES_ID_LENGDE) {
            return id;
        }
        return REFUNDERES_ID_PREFIX + id;
    }

    public static String endreTilNiSiffer(String refunderesId) {
        if (refunderesId.length() == REFUNDERES_ID_LENGDE) {
            return refunderesId.substring(2);
        }
        return refunderesId;
    }
}

