package no.nav.foreldrepenger.ytelse.beregning.adapter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

final class AktivitetStatusMapper {

    private static final Map<no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus, AktivitetStatus> REGEL_TIL_VL_MAP;
    private static final Map<AktivitetStatus, no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus> VL_TIL_REGEL_MAP;
    private static final Map<no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus, AktivitetStatus> BG_TIL_VL_MAP;

    private AktivitetStatusMapper() {
        //"Statisk" klasse
    }

    static {
        Map<no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus, AktivitetStatus> map = new EnumMap<>(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.class);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.AAP, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.BA, AktivitetStatus.BRUKERS_ANDEL);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.DP, AktivitetStatus.DAGPENGER);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.MS, AktivitetStatus.MILITÆR_ELLER_SIVIL);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.SN, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        map.put(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.KUN_YTELSE, AktivitetStatus.KUN_YTELSE);
        REGEL_TIL_VL_MAP = Collections.unmodifiableMap(map);
    }

    static {
        Map<AktivitetStatus, no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus> map = new HashMap<>();
        REGEL_TIL_VL_MAP.forEach((key, value) -> map.put(value, key)); //Initialiser reversert map
        map.put(AktivitetStatus.ARBEIDSTAKER, no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.ATFL);
        map.put(AktivitetStatus.FRILANSER, no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.ATFL);
        map.put(AktivitetStatus.KOMBINERT_AT_FL, no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.ATFL);
        map.put(AktivitetStatus.KOMBINERT_AT_FL_SN, no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.ATFL_SN);
        map.put(AktivitetStatus.KOMBINERT_AT_SN, no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.ATFL_SN);
        map.put(AktivitetStatus.KOMBINERT_FL_SN, no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.ATFL_SN);
        VL_TIL_REGEL_MAP = Collections.unmodifiableMap(map);
    }

    static {
        BG_TIL_VL_MAP = new HashMap<>();
        BG_TIL_VL_MAP.put(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.ARBEIDSAVKLARINGSPENGER, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
        BG_TIL_VL_MAP.put(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.DAGPENGER, AktivitetStatus.DAGPENGER);
        BG_TIL_VL_MAP.put(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.FRILANSER, AktivitetStatus.FRILANSER);
        BG_TIL_VL_MAP.put(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.ARBEIDSTAKER);
        BG_TIL_VL_MAP.put(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        BG_TIL_VL_MAP.put(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.KUN_YTELSE, AktivitetStatus.KUN_YTELSE);
        BG_TIL_VL_MAP.put(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.BRUKERS_ANDEL, AktivitetStatus.BRUKERS_ANDEL);
        BG_TIL_VL_MAP.put(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.MILITÆR_ELLER_SIVIL, AktivitetStatus.MILITÆR_ELLER_SIVIL);
        BG_TIL_VL_MAP.put(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.VENTELØNN_VARTPENGER, AktivitetStatus.VENTELØNN_VARTPENGER);
        BG_TIL_VL_MAP.put(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.TTLSTØTENDE_YTELSE, AktivitetStatus.TTLSTØTENDE_YTELSE);
    }

    static AktivitetStatus fraRegelTilVl(BeregningsresultatAndel andel) {
        if (andel.getAktivitetStatus().equals(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.ATFL)) {
            Arbeidsforhold arbeidsforhold = andel.getArbeidsforhold();
            return arbeidsforhold != null && arbeidsforhold.erFrilanser() ? AktivitetStatus.FRILANSER : AktivitetStatus.ARBEIDSTAKER;
        }
        if (REGEL_TIL_VL_MAP.containsKey(andel.getAktivitetStatus())) {
            return REGEL_TIL_VL_MAP.get(andel.getAktivitetStatus());
        }
        throw new IllegalArgumentException("Ukjent AktivitetStatus " + andel.getAktivitetStatus().name());
    }

    static no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus fraVLTilRegel(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus vlAktivitetStatus) {
        if (VL_TIL_REGEL_MAP.containsKey(AktivitetStatus.fraKode(vlAktivitetStatus.getKode()))) {
            return VL_TIL_REGEL_MAP.get(AktivitetStatus.fraKode(vlAktivitetStatus.getKode()));
        }
        throw new IllegalArgumentException("Ukjent AktivitetStatus " + vlAktivitetStatus);
    }

    static AktivitetStatus fraBGTilVL(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus bgAktivitetStatus) {
        if (BG_TIL_VL_MAP.containsKey(bgAktivitetStatus)) {
            return BG_TIL_VL_MAP.get(bgAktivitetStatus);
        }
        throw new IllegalArgumentException("Ukjent AktivitetStatus " + bgAktivitetStatus);
    }

}
