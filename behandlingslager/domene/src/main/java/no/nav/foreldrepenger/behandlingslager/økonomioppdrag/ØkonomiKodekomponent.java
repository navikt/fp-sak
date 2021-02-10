package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

// Etter avtale med Økonomi/OS
public enum ØkonomiKodekomponent {
    VLFP("VLFP"),
    OS("OS")
    ;

    private String kode;

    ØkonomiKodekomponent(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
