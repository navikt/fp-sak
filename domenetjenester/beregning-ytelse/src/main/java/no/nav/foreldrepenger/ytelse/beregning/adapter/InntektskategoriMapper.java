package no.nav.foreldrepenger.ytelse.beregning.adapter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;

final class InntektskategoriMapper {

    private static final Map<no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori, Inntektskategori> REGEL_TIL_VL_MAP;
    private static final Map<Inntektskategori, no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori> VL_TIL_REGEL_MAP;

    private InntektskategoriMapper() {
        //"Statisk" klasse
    }

    static {
        Map<no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori, Inntektskategori> map = new EnumMap<>(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.class);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.FRILANSER, Inntektskategori.FRILANSER);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.DAGPENGER, Inntektskategori.DAGPENGER);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.ARBEIDSAVKLARINGSPENGER, Inntektskategori.ARBEIDSAVKLARINGSPENGER);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.SJØMANN, Inntektskategori.SJØMANN);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.DAGMAMMA, Inntektskategori.DAGMAMMA);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.JORDBRUKER, Inntektskategori.JORDBRUKER);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.FISKER, Inntektskategori.FISKER);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER);
        REGEL_TIL_VL_MAP = Collections.unmodifiableMap(map);
    }

    static {
        Map<Inntektskategori, no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori> vLTilRegelMap = new HashMap<>();
        REGEL_TIL_VL_MAP.forEach((key, value) -> vLTilRegelMap.put(value, key)); //Initialiser reversert map
        VL_TIL_REGEL_MAP = Collections.unmodifiableMap(vLTilRegelMap);
    }

    static Inntektskategori fraRegelTilVL(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori inntektskategori) {
        return REGEL_TIL_VL_MAP.getOrDefault(inntektskategori, Inntektskategori.UDEFINERT);
    }

    static no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori fraVLTilRegel(no.nav.foreldrepenger.domene.entiteter.Inntektskategori inntektskategori) {
        return VL_TIL_REGEL_MAP.getOrDefault(Inntektskategori.fraKode(inntektskategori.getKode()), no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori.UDEFINERT);
    }
}
