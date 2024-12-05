package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode;

public enum GraderingAktivitetType  {
    ARBEID("Arbeid"),
    SELVSTENDIG_NÆRINGSDRIVENDE("Selvstendig næringsdrivende"),
    FRILANS("Frilans");

    private final String navn;

    GraderingAktivitetType(String navn) {
        this.navn = navn;
    }

    public String getNavn() {
        return navn;
    }

    public static GraderingAktivitetType from(boolean erArbeidstaker, boolean erFrilanser, boolean erSelvstNæringsdrivende) {
        if (erArbeidstaker) {
            return ARBEID;
        }
        if (erFrilanser) {
            return FRILANS;
        }
        if (erSelvstNæringsdrivende) {
            return SELVSTENDIG_NÆRINGSDRIVENDE;
        }
        return null;
    }
}
