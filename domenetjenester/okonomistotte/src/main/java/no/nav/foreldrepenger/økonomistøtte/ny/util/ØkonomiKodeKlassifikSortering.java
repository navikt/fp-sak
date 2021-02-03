package no.nav.foreldrepenger.økonomistøtte.ny.util;

import java.util.Arrays;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;

public class ØkonomiKodeKlassifikSortering {

    private static final List<String> SUFFIX_SORTERING = Arrays.asList(
        "ATORD", "ATAL", "ATFRI", "ATSJO",
        "SND-OP", "SNDDM-OP", "SNDJB-OP", "SNDFI",
        "FRISINN-FRILANS", "FRISINN-SELVST-OP",
        "REFAG-IOP",
        "FER", "FER-IOP", "FERPP-IOP"
    );

    public static int getSorteringsplassering(ØkonomiKodeKlassifik økonomiKodeKlassifik) {
        for (int i = 0; i < SUFFIX_SORTERING.size(); i++) {
            if (økonomiKodeKlassifik.getKodeKlassifik().endsWith(SUFFIX_SORTERING.get(i))) {
                return i;
            }
        }
        throw new IllegalArgumentException("Ikke-definert sorteringsplassering for " + økonomiKodeKlassifik);
    }
}
