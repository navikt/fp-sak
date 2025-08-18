package no.nav.foreldrepenger.ytelse.beregning.adapter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;

final class InntektskategoriMapper {

    private static final Map<no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori, Inntektskategori> REGEL_TIL_VL_MAP = Map.ofEntries(
        Map.entry(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER),
        Map.entry(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.FRILANSER, Inntektskategori.FRILANSER),
        Map.entry(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE),
        Map.entry(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.DAGPENGER, Inntektskategori.DAGPENGER),
        Map.entry(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.ARBEIDSAVKLARINGSPENGER, Inntektskategori.ARBEIDSAVKLARINGSPENGER),
        Map.entry(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.SJØMANN, Inntektskategori.SJØMANN),
        Map.entry(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.DAGMAMMA, Inntektskategori.DAGMAMMA),
        Map.entry(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.JORDBRUKER, Inntektskategori.JORDBRUKER),
        Map.entry(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.FISKER, Inntektskategori.FISKER),
        Map.entry(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER)
    );
    private static final Map<no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori, Inntektskategori> DOMENE_TIL_VL_MAP = Map.ofEntries(
        Map.entry(no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER),
        Map.entry(no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori.FRILANSER, Inntektskategori.FRILANSER),
        Map.entry(no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE),
        Map.entry(no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori.DAGPENGER, Inntektskategori.DAGPENGER),
        Map.entry(no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori.ARBEIDSAVKLARINGSPENGER, Inntektskategori.ARBEIDSAVKLARINGSPENGER),
        Map.entry(no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori.SJØMANN, Inntektskategori.SJØMANN),
        Map.entry(no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori.DAGMAMMA, Inntektskategori.DAGMAMMA),
        Map.entry(no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori.JORDBRUKER, Inntektskategori.JORDBRUKER),
        Map.entry(no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori.FISKER, Inntektskategori.FISKER),
        Map.entry(no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER)
    );
    private static final Map<Inntektskategori, no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori> VL_TIL_REGEL_MAP;

    private InntektskategoriMapper() {
        //"Statisk" klasse
    }
    static {
        Map<Inntektskategori, no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori> vLTilRegelMap = new EnumMap<>(Inntektskategori.class);
        REGEL_TIL_VL_MAP.forEach((key, value) -> vLTilRegelMap.put(value, key)); //Initialiser reversert map
        VL_TIL_REGEL_MAP = Collections.unmodifiableMap(vLTilRegelMap);
    }

    static Inntektskategori fraRegelTilVL(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori inntektskategori) {
        return REGEL_TIL_VL_MAP.getOrDefault(inntektskategori, Inntektskategori.UDEFINERT);
    }

    static no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori fraVLTilRegel(no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori inntektskategori) {
        var behandlingslagerIK = DOMENE_TIL_VL_MAP.getOrDefault(inntektskategori, Inntektskategori.UDEFINERT);
        return VL_TIL_REGEL_MAP.getOrDefault(behandlingslagerIK, no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.UDEFINERT);
    }
}
