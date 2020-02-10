package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;

public class InntektskategoriKlassekodeMapper {

    private static final Map<Inntektskategori, ØkonomiKodeKlassifik> INNTEKTSKATEGORI_KLASSEKODE_MAP_FØDSEL = Map.of(
        Inntektskategori.ARBEIDSTAKER, ØkonomiKodeKlassifik.FPATORD,
        Inntektskategori.FRILANSER, ØkonomiKodeKlassifik.FPATFRI,
        Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, ØkonomiKodeKlassifik.FPSND_OP,
        Inntektskategori.DAGPENGER, ØkonomiKodeKlassifik.FPATAL,
        Inntektskategori.ARBEIDSAVKLARINGSPENGER, ØkonomiKodeKlassifik.FPATAL,
        Inntektskategori.SJØMANN, ØkonomiKodeKlassifik.FPATSJO,
        Inntektskategori.DAGMAMMA, ØkonomiKodeKlassifik.FPSNDDM_OP,
        Inntektskategori.JORDBRUKER, ØkonomiKodeKlassifik.FPSNDJB_OP,
        Inntektskategori.FISKER, ØkonomiKodeKlassifik.FPSNDFI,
        Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, ØkonomiKodeKlassifik.FPATORD);

    private static final Map<Inntektskategori, ØkonomiKodeKlassifik> INNTEKTSKATEGORI_KLASSEKODE_MAP_ADOPSJON = Map.of(
        Inntektskategori.ARBEIDSTAKER, ØkonomiKodeKlassifik.FPADATORD,
        Inntektskategori.FRILANSER, ØkonomiKodeKlassifik.FPADATFRI,
        Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, ØkonomiKodeKlassifik.FPADSND_OP,
        Inntektskategori.DAGPENGER, ØkonomiKodeKlassifik.FPADATAL,
        Inntektskategori.ARBEIDSAVKLARINGSPENGER, ØkonomiKodeKlassifik.FPADATAL,
        Inntektskategori.SJØMANN, ØkonomiKodeKlassifik.FPADATSJO,
        Inntektskategori.DAGMAMMA, ØkonomiKodeKlassifik.FPADSNDDM_OP,
        Inntektskategori.JORDBRUKER, ØkonomiKodeKlassifik.FPADSNDJB_OP,
        Inntektskategori.FISKER, ØkonomiKodeKlassifik.FPADSNDFI,
        Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, ØkonomiKodeKlassifik.FPADATORD);

    private static final Map<Inntektskategori, ØkonomiKodeKlassifik> INNTEKTSKATEGORI_KLASSEKODE_MAP_SVANGERSKAPSPENGER = Map.of(
        Inntektskategori.ARBEIDSTAKER, ØkonomiKodeKlassifik.FPSVATORD,
        Inntektskategori.FRILANSER, ØkonomiKodeKlassifik.FPSVATFRI,
        Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, ØkonomiKodeKlassifik.FPSVSND_OP,
        Inntektskategori.DAGPENGER, ØkonomiKodeKlassifik.FPSVATAL,
        Inntektskategori.ARBEIDSAVKLARINGSPENGER, ØkonomiKodeKlassifik.FPSVATAL,
        Inntektskategori.SJØMANN, ØkonomiKodeKlassifik.FPSVATSJO,
        Inntektskategori.DAGMAMMA, ØkonomiKodeKlassifik.FPSVSNDDM_OP,
        Inntektskategori.JORDBRUKER, ØkonomiKodeKlassifik.FPSVSNDJB_OP,
        Inntektskategori.FISKER, ØkonomiKodeKlassifik.FPSVSNDFI,
        Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, ØkonomiKodeKlassifik.FPSVATORD);

    private InntektskategoriKlassekodeMapper() {
        //for å hindre instansiering
    }

    public static String mapTilKlassekode(Inntektskategori inntektskategori, FamilieYtelseType familieYtelseType) {
        var map = getMap(familieYtelseType);
        verifyInntektskategori(map, inntektskategori);
        return map.get(inntektskategori).getKodeKlassifik();
    }

    public static void verifyInntektskategori(Inntektskategori inntektskategori) {
        verifyInntektskategori(INNTEKTSKATEGORI_KLASSEKODE_MAP_ADOPSJON, inntektskategori);
        verifyInntektskategori(INNTEKTSKATEGORI_KLASSEKODE_MAP_FØDSEL, inntektskategori);
        verifyInntektskategori(INNTEKTSKATEGORI_KLASSEKODE_MAP_SVANGERSKAPSPENGER, inntektskategori);
    }

    private static void verifyInntektskategori(Map<Inntektskategori, ØkonomiKodeKlassifik> map, Inntektskategori inntektskategori) {
        // Map keys er samme for alle tre, det er nok å sjekke en av dem
        if (!map.containsKey(inntektskategori)) {
            throw new IllegalStateException("Utvikler feil: Mangler mapping for inntektskategori " + inntektskategori);
        }
    }

    private static Map<Inntektskategori, ØkonomiKodeKlassifik> getMap(FamilieYtelseType type) {
        switch (type) {
            case FØDSEL:
                return INNTEKTSKATEGORI_KLASSEKODE_MAP_FØDSEL;
            case ADOPSJON:
                return INNTEKTSKATEGORI_KLASSEKODE_MAP_ADOPSJON;
            case SVANGERSKAPSPENGER:
                return INNTEKTSKATEGORI_KLASSEKODE_MAP_SVANGERSKAPSPENGER;
            default:
                throw new IllegalArgumentException("Støtter ikke FamilieYtelseType: " + type);
        }
    }
}
