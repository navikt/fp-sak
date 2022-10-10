package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode;

public enum GraderingAktivitetType {
    ARBEID,
    SELVSTENDIG_NÆRINGSDRIVENDE,
    FRILANS,
    ;

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
