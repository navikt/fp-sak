package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;

public class InntektskategoriKlassekodeMapper {

    private static final Map<Inntektskategori, KodeKlassifik> INNTEKTSKATEGORI_KLASSEKODE_MAP_FØDSEL = Map.of(Inntektskategori.ARBEIDSTAKER,
        KodeKlassifik.FPF_ARBEIDSTAKER, Inntektskategori.FRILANSER, KodeKlassifik.FPF_FRILANSER, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE,
        KodeKlassifik.FPF_SELVSTENDIG, Inntektskategori.DAGPENGER, KodeKlassifik.FPF_DAGPENGER, Inntektskategori.ARBEIDSAVKLARINGSPENGER,
        KodeKlassifik.FPF_DAGPENGER, Inntektskategori.SJØMANN, KodeKlassifik.FPF_SJØMANN, Inntektskategori.DAGMAMMA, KodeKlassifik.FPF_DAGMAMMA,
        Inntektskategori.JORDBRUKER, KodeKlassifik.FPF_JORDBRUKER, Inntektskategori.FISKER, KodeKlassifik.FPF_FISKER,
        Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, KodeKlassifik.FPF_ARBEIDSTAKER);

    private static final Map<Inntektskategori, KodeKlassifik> INNTEKTSKATEGORI_KLASSEKODE_MAP_ADOPSJON = Map.of(Inntektskategori.ARBEIDSTAKER,
        KodeKlassifik.FPA_ARBEIDSTAKER, Inntektskategori.FRILANSER, KodeKlassifik.FPA_FRILANSER, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE,
        KodeKlassifik.FPA_SELVSTENDIG, Inntektskategori.DAGPENGER, KodeKlassifik.FPA_DAGPENGER, Inntektskategori.ARBEIDSAVKLARINGSPENGER,
        KodeKlassifik.FPA_DAGPENGER, Inntektskategori.SJØMANN, KodeKlassifik.FPA_SJØMANN, Inntektskategori.DAGMAMMA, KodeKlassifik.FPA_DAGMAMMA,
        Inntektskategori.JORDBRUKER, KodeKlassifik.FPA_JORDBRUKER, Inntektskategori.FISKER, KodeKlassifik.FPA_FISKER,
        Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, KodeKlassifik.FPA_ARBEIDSTAKER);

    private static final Map<Inntektskategori, KodeKlassifik> INNTEKTSKATEGORI_KLASSEKODE_MAP_SVANGERSKAPSPENGER = Map.of(
        Inntektskategori.ARBEIDSTAKER, KodeKlassifik.SVP_ARBEDISTAKER, Inntektskategori.FRILANSER, KodeKlassifik.SVP_FRILANSER,
        Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, KodeKlassifik.SVP_SELVSTENDIG, Inntektskategori.DAGPENGER, KodeKlassifik.SVP_DAGPENGER,
        Inntektskategori.ARBEIDSAVKLARINGSPENGER, KodeKlassifik.SVP_DAGPENGER, Inntektskategori.SJØMANN, KodeKlassifik.SVP_SJØMANN,
        Inntektskategori.DAGMAMMA, KodeKlassifik.SVP_DAGMAMMA, Inntektskategori.JORDBRUKER, KodeKlassifik.SVP_JORDBRUKER, Inntektskategori.FISKER,
        KodeKlassifik.SVP_FISKER, Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, KodeKlassifik.SVP_ARBEDISTAKER);

    private InntektskategoriKlassekodeMapper() {
        //for å hindre instansiering
    }

    public static KodeKlassifik mapTilKlassekode(Inntektskategori inntektskategori, FamilieYtelseType familieYtelseType) {
        var map = getMap(familieYtelseType);
        verifyInntektskategori(map, inntektskategori);
        return map.get(inntektskategori);
    }

    public static void verifyInntektskategori(Inntektskategori inntektskategori) {
        verifyInntektskategori(INNTEKTSKATEGORI_KLASSEKODE_MAP_ADOPSJON, inntektskategori);
        verifyInntektskategori(INNTEKTSKATEGORI_KLASSEKODE_MAP_FØDSEL, inntektskategori);
        verifyInntektskategori(INNTEKTSKATEGORI_KLASSEKODE_MAP_SVANGERSKAPSPENGER, inntektskategori);
    }

    private static void verifyInntektskategori(Map<Inntektskategori, KodeKlassifik> map, Inntektskategori inntektskategori) {
        // Map keys er samme for alle tre, det er nok å sjekke en av dem
        if (!map.containsKey(inntektskategori)) {
            throw new IllegalStateException("Utvikler feil: Mangler mapping for inntektskategori " + inntektskategori);
        }
    }

    private static Map<Inntektskategori, KodeKlassifik> getMap(FamilieYtelseType type) {
        return switch (type) {
            case FØDSEL -> INNTEKTSKATEGORI_KLASSEKODE_MAP_FØDSEL;
            case ADOPSJON -> INNTEKTSKATEGORI_KLASSEKODE_MAP_ADOPSJON;
            case SVANGERSKAPSPENGER -> INNTEKTSKATEGORI_KLASSEKODE_MAP_SVANGERSKAPSPENGER;
        };
    }
}
