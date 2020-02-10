package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

public enum ØkonomiKodeKlassifik {
    //både adopsjon og fødsel
    FPATFER("FPATFER"), // Bruker - Feriepenger.

    //fødsel
    FPATORD("FPATORD"), // FP (foreldrepenger), AT - arbeidstaker, ORD - ordinær
    FPATFRI("FPATFRI"),
    FPSND_OP("FPSND-OP"),
    FPATAL("FPATAL"),
    FPATSJO("FPATSJO"),
    FPSNDDM_OP("FPSNDDM-OP"),
    FPSNDJB_OP("FPSNDJB-OP"),
    FPSNDFI("FPSNDFI"),
    FPREFAG_IOP("FPREFAG-IOP"), //FP (foreldrepenger), REFAG - arbeidsgiver
    FPREFAGFER_IOP("FPREFAGFER-IOP"), // Arbeidsgiver - Feriepenger

    //adopsjon
    FPADATORD("FPADATORD"), // FP (foreldrepenger), AD - adopsjon, AT - arbeidstaker, ORD - ordinær
    FPADATFRI("FPADATFRI"),
    FPADSND_OP("FPADSND-OP"),
    FPADATAL("FPADATAL"),
    FPADATSJO("FPADATSJO"),
    FPADSNDDM_OP("FPADSNDDM-OP"),
    FPADSNDJB_OP("FPADSNDJB-OP"),
    FPADSNDFI("FPADSNDFI"),
    FPADREFAG_IOP("FPADREFAG-IOP"), //FP (foreldrepenger), AD - adopsjon, REFAG - arbeidsgiver
    FPADREFAGFER_IOP("FPADREFAGFER-IOP"), // Arbeidsgiver - Feriepenger

    //svangerskapsenger
    FPSVATORD("FPSVATORD"), // FPSV (svangerskapsenger), AT - arbeidstaker, ORD - ordinær
    FPSVATFRI("FPSVATFRI"),
    FPSVSND_OP("FPSVSND-OP"),
    FPSVATAL("FPSVATAL"),
    FPSVATSJO("FPSVATSJO"),
    FPSVSNDDM_OP("FPSVSNDDM-OP"),
    FPSVSNDJB_OP("FPSVSNDJB-OP"),
    FPSVSNDFI("FPSVSNDFI"),
    FPSVREFAG_IOP("FPSVREFAG-IOP"), //FPSV (svangerskapsenger), REFAG - arbeidsgiver
    FPSVREFAGFER_IOP("FPSVREFAGFER-IOP"); // Arbeidsgiver - Feriepenger

    private String kodeKlassifik;

    ØkonomiKodeKlassifik(String kodeKlassifik) {
        this.kodeKlassifik = kodeKlassifik;
    }

    public static ØkonomiKodeKlassifik fraKode(String kode) {
        for (ØkonomiKodeKlassifik value : ØkonomiKodeKlassifik.values()) {
            if (value.getKodeKlassifik().equals(kode)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Har ingen ØkonomiKodeKlassifik for " + kode);
    }

    public String getKodeKlassifik() {
        return kodeKlassifik;
    }

    public boolean gjelderFerie() {
        return this == FPATFER
            || this == FPREFAGFER_IOP
            || this == FPADREFAGFER_IOP
            || this == FPSVREFAGFER_IOP;
    }
}
