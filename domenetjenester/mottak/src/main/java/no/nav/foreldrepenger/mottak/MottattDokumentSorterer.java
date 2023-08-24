package no.nav.foreldrepenger.mottak;

import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;

import java.util.Comparator;

class MottattDokumentSorterer {
    private static Comparator<String> nullSafeStringComparator = Comparator.nullsFirst(String::compareToIgnoreCase);

    private MottattDokumentSorterer() {
        // skjul public constructor
    }

    static Comparator<MottattDokument> sorterMottattDokument() {
        return Comparator.comparing(MottattDokument::getMottattDato)
            .thenComparing(MottattDokument::getKanalreferanse, nullSafeStringComparator);
    }
}
