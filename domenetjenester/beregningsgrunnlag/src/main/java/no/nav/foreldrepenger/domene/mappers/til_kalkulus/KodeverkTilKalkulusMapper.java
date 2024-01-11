package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import no.nav.folketrygdloven.kalkulator.modell.uttak.UttakArbeidType;
import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.AndelKilde;
import no.nav.folketrygdloven.kalkulus.kodeverk.Inntektskategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.SammenligningsgrunnlagType;

public class KodeverkTilKalkulusMapper {

    private KodeverkTilKalkulusMapper() {
        // Skjuler default konstruktør
    }

    public static Inntektskategori mapInntektskategori(no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori inntektskategori) {
        return switch (inntektskategori) {
            case ARBEIDSTAKER -> Inntektskategori.ARBEIDSTAKER;
            case FRILANSER -> Inntektskategori.FRILANSER;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE;
            case DAGPENGER -> Inntektskategori.DAGPENGER;
            case ARBEIDSAVKLARINGSPENGER -> Inntektskategori.ARBEIDSAVKLARINGSPENGER;
            case SJØMANN -> Inntektskategori.SJØMANN;
            case DAGMAMMA -> Inntektskategori.DAGMAMMA;
            case JORDBRUKER -> Inntektskategori.JORDBRUKER;
            case FISKER -> Inntektskategori.FISKER;
            case ARBEIDSTAKER_UTEN_FERIEPENGER -> Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER;
            case UDEFINERT -> Inntektskategori.UDEFINERT;
        };
    }

    public static AndelKilde mapAndelkilde(no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde andelKilde) {
        return switch (andelKilde) {
            case SAKSBEHANDLER_KOFAKBER -> AndelKilde.SAKSBEHANDLER_KOFAKBER;
            case PROSESS_BESTEBEREGNING -> AndelKilde.PROSESS_BESTEBEREGNING;
            case SAKSBEHANDLER_FORDELING -> AndelKilde.SAKSBEHANDLER_FORDELING;
            case PROSESS_PERIODISERING -> AndelKilde.PROSESS_PERIODISERING;
            case PROSESS_OMFORDELING -> AndelKilde.PROSESS_OMFORDELING;
            case PROSESS_START -> AndelKilde.PROSESS_START;
        };
    }

    public static AktivitetStatus mapAktivitetstatus(no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus aktivitetStatus) {
        return switch (aktivitetStatus) {
            case ARBEIDSAVKLARINGSPENGER -> AktivitetStatus.ARBEIDSAVKLARINGSPENGER;
            case ARBEIDSTAKER -> AktivitetStatus.ARBEIDSTAKER;
            case UDEFINERT -> AktivitetStatus.UDEFINERT;
            case DAGPENGER -> AktivitetStatus.DAGPENGER;
            case FRILANSER -> AktivitetStatus.FRILANSER;
            case MILITÆR_ELLER_SIVIL -> AktivitetStatus.MILITÆR_ELLER_SIVIL;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
            case VENTELØNN_VARTPENGER -> AktivitetStatus.VENTELØNN_VARTPENGER;
            case KOMBINERT_AT_FL -> AktivitetStatus.KOMBINERT_AT_FL;
            case KOMBINERT_AT_SN -> AktivitetStatus.KOMBINERT_AT_SN;
            case KOMBINERT_FL_SN -> AktivitetStatus.KOMBINERT_FL_SN;
            case KOMBINERT_AT_FL_SN -> AktivitetStatus.KOMBINERT_AT_FL_SN;
            case BRUKERS_ANDEL -> AktivitetStatus.BRUKERS_ANDEL;
            case KUN_YTELSE -> AktivitetStatus.KUN_YTELSE;
            case TTLSTØTENDE_YTELSE -> AktivitetStatus.TTLSTØTENDE_YTELSE;
        };
    }

    public static SammenligningsgrunnlagType mapSammenligningsgrunnlagtype(no.nav.foreldrepenger.domene.modell.kodeverk.SammenligningsgrunnlagType sammenligningsgrunnlagType) {
        return switch (sammenligningsgrunnlagType) {
            case SAMMENLIGNING_AT -> SammenligningsgrunnlagType.SAMMENLIGNING_AT;
            case SAMMENLIGNING_FL -> SammenligningsgrunnlagType.SAMMENLIGNING_FL;
            case SAMMENLIGNING_AT_FL -> SammenligningsgrunnlagType.SAMMENLIGNING_AT_FL;
            case SAMMENLIGNING_SN -> SammenligningsgrunnlagType.SAMMENLIGNING_SN;
            case SAMMENLIGNING_ATFL_SN -> SammenligningsgrunnlagType.SAMMENLIGNING_ATFL_SN;
        };
    }

    public static UttakArbeidType mapUttakArbeidType(no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType uttakArbeidType) {
        return switch (uttakArbeidType) {
            case ORDINÆRT_ARBEID -> UttakArbeidType.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> UttakArbeidType.FRILANS;
            case ANNET -> throw new IllegalStateException("Fikk inn uttaktype ANNET under mapping av tilrettelegginger, ugyldig tilstand");
        };
    }

}
