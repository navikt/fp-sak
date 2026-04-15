package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

enum OversiktAktivitetStatus {
    ARBEIDSAVKLARINGSPENGER,
    ARBEIDSTAKER,
    DAGPENGER,
    FRILANSER,
    MILITÆR_ELLER_SIVIL,
    SELVSTENDIG_NÆRINGSDRIVENDE,
    KOMBINERT_AT_FL,
    KOMBINERT_AT_SN,
    KOMBINERT_FL_SN,
    KOMBINERT_AT_FL_SN,
    BRUKERS_ANDEL,
    KUN_YTELSE;

    public static OversiktAktivitetStatus fraBehandlingslagerStatus(no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus aktivitetStatus) {
        return switch (aktivitetStatus) {
            case ARBEIDSAVKLARINGSPENGER -> OversiktAktivitetStatus.ARBEIDSAVKLARINGSPENGER;
            case ARBEIDSTAKER -> OversiktAktivitetStatus.ARBEIDSTAKER;
            case DAGPENGER -> OversiktAktivitetStatus.DAGPENGER;
            case FRILANSER -> OversiktAktivitetStatus.FRILANSER;
            case MILITÆR_ELLER_SIVIL -> OversiktAktivitetStatus.MILITÆR_ELLER_SIVIL;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> OversiktAktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
            case KOMBINERT_AT_FL -> OversiktAktivitetStatus.KOMBINERT_AT_FL;
            case KOMBINERT_AT_SN -> OversiktAktivitetStatus.KOMBINERT_AT_SN;
            case KOMBINERT_FL_SN -> OversiktAktivitetStatus.KOMBINERT_FL_SN;
            case KOMBINERT_AT_FL_SN -> OversiktAktivitetStatus.KOMBINERT_AT_FL_SN;
            case BRUKERS_ANDEL -> OversiktAktivitetStatus.BRUKERS_ANDEL;
            case KUN_YTELSE -> OversiktAktivitetStatus.KUN_YTELSE;
            case VENTELØNN_VARTPENGER, TTLSTØTENDE_YTELSE, UDEFINERT -> null;
        };
    }
}
