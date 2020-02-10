package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

// Etter avtale med Økonomi/OS
public enum ØkonomiKodekomponent {
    VLFP("VLFP"),
    OS("OS")
    ;

    private String kodekomponent;

    ØkonomiKodekomponent(String kodekomponent) {
        this.kodekomponent = kodekomponent;
    }

    public String getKodekomponent() {
        return kodekomponent;
    }
}
