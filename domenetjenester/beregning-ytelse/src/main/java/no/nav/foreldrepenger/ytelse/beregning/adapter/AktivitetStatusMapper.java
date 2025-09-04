package no.nav.foreldrepenger.ytelse.beregning.adapter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr;

final class AktivitetStatusMapper {

    private static final Map<no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus, AktivitetStatus> REGEL_TIL_VL_MAP;
    private static final Map<AktivitetStatus, no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus> VL_TIL_REGEL_MAP;

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

    static AktivitetStatus fraRegelTilVl(BeregningsresultatAndel andel) {
        if (andel.getAktivitetStatus().equals(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.ATFL)) {
            var arbeidsforhold = andel.getArbeidsforhold();
            return arbeidsforhold != null && arbeidsforhold.frilanser() ? AktivitetStatus.FRILANSER : AktivitetStatus.ARBEIDSTAKER;
        }
        if (REGEL_TIL_VL_MAP.containsKey(andel.getAktivitetStatus())) {
            return REGEL_TIL_VL_MAP.get(andel.getAktivitetStatus());
        }
        throw new IllegalArgumentException("Ukjent AktivitetStatus " + andel.getAktivitetStatus().name());
    }

    static AktivitetStatus fraRegelTilVl(BeregningsresultatFeriepengerPrÅr feriepenger) {
        if (feriepenger.getAktivitetStatus().equals(no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus.ATFL)) {
            var arbeidsforhold = feriepenger.getArbeidsforhold();
            return arbeidsforhold != null && arbeidsforhold.frilanser() ? AktivitetStatus.FRILANSER : AktivitetStatus.ARBEIDSTAKER;
        }
        if (REGEL_TIL_VL_MAP.containsKey(feriepenger.getAktivitetStatus())) {
            return REGEL_TIL_VL_MAP.get(feriepenger.getAktivitetStatus());
        }
        throw new IllegalArgumentException("Ukjent AktivitetStatus " + feriepenger.getAktivitetStatus().name());
    }

    static no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus fraVLTilRegel(no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus vlAktivitetStatus) {
        if (VL_TIL_REGEL_MAP.containsKey(AktivitetStatus.fraKode(vlAktivitetStatus.getKode()))) {
            return VL_TIL_REGEL_MAP.get(AktivitetStatus.fraKode(vlAktivitetStatus.getKode()));
        }
        throw new IllegalArgumentException("Ukjent AktivitetStatus " + vlAktivitetStatus);
    }
}
