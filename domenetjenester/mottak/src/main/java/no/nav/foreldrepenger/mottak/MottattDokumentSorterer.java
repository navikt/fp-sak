package no.nav.foreldrepenger.mottak;

import java.util.Comparator;

import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;

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
