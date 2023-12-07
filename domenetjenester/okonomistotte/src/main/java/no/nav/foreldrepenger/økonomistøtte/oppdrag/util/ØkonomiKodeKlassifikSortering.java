package no.nav.foreldrepenger.økonomistøtte.oppdrag.util;

import java.util.Arrays;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;

public class ØkonomiKodeKlassifikSortering {

    private static final List<String> SUFFIX_SORTERING = Arrays.asList(
        "ATORD", "ATAL", "ATFRI", "ATSJO",
        "SND-OP", "SNDDM-OP", "SNDJB-OP", "SNDFI",
        "REFAG-IOP",
        "SVATFER", "ADATFER", "FER", "FER-IOP", // For å skille feriepenger til bruker ifm migrering
        "ENFOD-OP", "ENAD-OP"
    );

    private ØkonomiKodeKlassifikSortering() {
    }

    public static int getSorteringsplassering(KodeKlassifik kodeKlassifik) {
        for (var i = 0; i < SUFFIX_SORTERING.size(); i++) {
            if (kodeKlassifik.getKode().endsWith(SUFFIX_SORTERING.get(i))) {
                return i;
            }
        }
        throw new IllegalArgumentException("Ikke-definert sorteringsplassering for " + kodeKlassifik);
    }
}
