package no.nav.foreldrepenger.domene.mappers.fra_kalkulator_til_entitet;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningAktivitetHandlingType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagPeriodeRegelType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;
import no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.domene.modell.kodeverk.PeriodeÅrsak;
import no.nav.foreldrepenger.domene.modell.kodeverk.SammenligningsgrunnlagType;

public class KodeverkFraKalkulusMapper {

    private KodeverkFraKalkulusMapper() {
        // Skjuler default konstruktør
    }

    public static AktivitetStatus mapAktivitetstatus(no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus aks) {
        return switch(aks) {
            case ARBEIDSAVKLARINGSPENGER -> AktivitetStatus.ARBEIDSAVKLARINGSPENGER;
            case ARBEIDSTAKER -> AktivitetStatus.ARBEIDSTAKER;
            case DAGPENGER -> AktivitetStatus.DAGPENGER;
            case FRILANSER -> AktivitetStatus.FRILANSER;
            case MILITÆR_ELLER_SIVIL -> AktivitetStatus.MILITÆR_ELLER_SIVIL;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
            case KOMBINERT_AT_FL -> AktivitetStatus.KOMBINERT_AT_FL;
            case KOMBINERT_AT_SN -> AktivitetStatus.KOMBINERT_AT_SN;
            case KOMBINERT_FL_SN -> AktivitetStatus.KOMBINERT_FL_SN;
            case KOMBINERT_AT_FL_SN -> AktivitetStatus.KOMBINERT_AT_FL_SN;
            case BRUKERS_ANDEL -> AktivitetStatus.BRUKERS_ANDEL;
            case KUN_YTELSE -> AktivitetStatus.KUN_YTELSE;
            case VENTELØNN_VARTPENGER -> AktivitetStatus.VENTELØNN_VARTPENGER;
            case UDEFINERT -> AktivitetStatus.UDEFINERT;
            case MIDLERTIDIG_INAKTIV, SYKEPENGER_AV_DAGPENGER, PLEIEPENGER_AV_DAGPENGER, TTLSTØTENDE_YTELSE -> throw new IllegalStateException(ukjentKodeFeil("aktivitetstatus", aks.getKode()));
        };
    }

    public static FaktaOmBeregningTilfelle mapFaktaTilfelle(no.nav.folketrygdloven.kalkulus.kodeverk.FaktaOmBeregningTilfelle faktaOmBeregningTilfelle) {
        return switch (faktaOmBeregningTilfelle) {
            case UDEFINERT -> FaktaOmBeregningTilfelle.UDEFINERT;
            case VURDER_TIDSBEGRENSET_ARBEIDSFORHOLD -> FaktaOmBeregningTilfelle.VURDER_TIDSBEGRENSET_ARBEIDSFORHOLD;
            case VURDER_SN_NY_I_ARBEIDSLIVET -> FaktaOmBeregningTilfelle.VURDER_SN_NY_I_ARBEIDSLIVET;
            case VURDER_NYOPPSTARTET_FL -> FaktaOmBeregningTilfelle.VURDER_NYOPPSTARTET_FL;
            case FASTSETT_MAANEDSINNTEKT_FL -> FaktaOmBeregningTilfelle.FASTSETT_MAANEDSINNTEKT_FL;
            case FASTSETT_BG_ARBEIDSTAKER_UTEN_INNTEKTSMELDING -> FaktaOmBeregningTilfelle.FASTSETT_BG_ARBEIDSTAKER_UTEN_INNTEKTSMELDING;
            case VURDER_LØNNSENDRING -> FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING;
            case FASTSETT_MÅNEDSLØNN_ARBEIDSTAKER_UTEN_INNTEKTSMELDING -> FaktaOmBeregningTilfelle.FASTSETT_MÅNEDSLØNN_ARBEIDSTAKER_UTEN_INNTEKTSMELDING;
            case VURDER_AT_OG_FL_I_SAMME_ORGANISASJON -> FaktaOmBeregningTilfelle.VURDER_AT_OG_FL_I_SAMME_ORGANISASJON;
            case FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE -> FaktaOmBeregningTilfelle.FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE;
            case VURDER_ETTERLØNN_SLUTTPAKKE -> FaktaOmBeregningTilfelle.VURDER_ETTERLØNN_SLUTTPAKKE;
            case FASTSETT_ETTERLØNN_SLUTTPAKKE -> FaktaOmBeregningTilfelle.FASTSETT_ETTERLØNN_SLUTTPAKKE;
            case VURDER_MOTTAR_YTELSE -> FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE;
            case VURDER_BESTEBEREGNING -> FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING;
            case VURDER_MILITÆR_SIVILTJENESTE -> FaktaOmBeregningTilfelle.VURDER_MILITÆR_SIVILTJENESTE;
            case VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT -> FaktaOmBeregningTilfelle.VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT;
            case FASTSETT_BG_KUN_YTELSE -> FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE;
            case TILSTØTENDE_YTELSE -> FaktaOmBeregningTilfelle.TILSTØTENDE_YTELSE;
            case FASTSETT_ENDRET_BEREGNINGSGRUNNLAG -> FaktaOmBeregningTilfelle.FASTSETT_ENDRET_BEREGNINGSGRUNNLAG;
        };
    }

    public static PeriodeÅrsak mapPeriodeÅrsak(no.nav.folketrygdloven.kalkulus.kodeverk.PeriodeÅrsak periodeÅrsak) {
        return switch (periodeÅrsak) {
            case UDEFINERT -> PeriodeÅrsak.UDEFINERT;
            case NATURALYTELSE_BORTFALT -> PeriodeÅrsak.NATURALYTELSE_BORTFALT;
            case ARBEIDSFORHOLD_AVSLUTTET -> PeriodeÅrsak.ARBEIDSFORHOLD_AVSLUTTET;
            case NATURALYTELSE_TILKOMMER -> PeriodeÅrsak.NATURALYTELSE_TILKOMMER;
            case ENDRING_I_REFUSJONSKRAV -> PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV;
            case REFUSJON_OPPHØRER -> PeriodeÅrsak.REFUSJON_OPPHØRER;
            case GRADERING -> PeriodeÅrsak.GRADERING;
            case GRADERING_OPPHØRER -> PeriodeÅrsak.GRADERING_OPPHØRER;
            case ENDRING_I_AKTIVITETER_SØKT_FOR -> PeriodeÅrsak.ENDRING_I_AKTIVITETER_SØKT_FOR;
            case REFUSJON_AVSLÅTT -> PeriodeÅrsak.REFUSJON_AVSLÅTT;
            case TILKOMMET_INNTEKT, REPRESENTERER_STORTINGET_AVSLUTTET, REPRESENTERER_STORTINGET,
                TILKOMMET_INNTEKT_AVSLUTTET, TILKOMMET_INNTEKT_MANUELT -> throw new IllegalStateException(ukjentKodeFeil("PeriodeÅrsak", periodeÅrsak.getKode()));
        };
    }

    public static OpptjeningAktivitetType mapOpptjeningtype(no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType opptjeningtype) {
        return switch (opptjeningtype) {
            case AAP -> OpptjeningAktivitetType.ARBEIDSAVKLARING;
            case ARBEID -> OpptjeningAktivitetType.ARBEID;
            case DAGPENGER -> OpptjeningAktivitetType.DAGPENGER;
            case FORELDREPENGER -> OpptjeningAktivitetType.FORELDREPENGER;
            case FRILANS -> OpptjeningAktivitetType.FRILANS;
            case MILITÆR_ELLER_SIVILTJENESTE -> OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE;
            case NÆRING -> OpptjeningAktivitetType.NÆRING;
            case OMSORGSPENGER -> OpptjeningAktivitetType.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> OpptjeningAktivitetType.OPPLÆRINGSPENGER;
            case PLEIEPENGER -> OpptjeningAktivitetType.PLEIEPENGER;
            case FRISINN -> OpptjeningAktivitetType.FRISINN;
            case ETTERLØNN_SLUTTPAKKE -> OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE;
            case SVANGERSKAPSPENGER -> OpptjeningAktivitetType.SVANGERSKAPSPENGER;
            case SYKEPENGER -> OpptjeningAktivitetType.SYKEPENGER;
            case VENTELØNN_VARTPENGER -> OpptjeningAktivitetType.VENTELØNN_VARTPENGER;
            case VIDERE_ETTERUTDANNING -> OpptjeningAktivitetType.VIDERE_ETTERUTDANNING;
            case UTENLANDSK_ARBEIDSFORHOLD -> OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD;
            case UTDANNINGSPERMISJON -> OpptjeningAktivitetType.UTDANNINGSPERMISJON;
            case UDEFINERT -> OpptjeningAktivitetType.UDEFINERT;
            case SYKEPENGER_AV_DAGPENGER, PLEIEPENGER_AV_DAGPENGER -> throw new IllegalStateException(ukjentKodeFeil("opptjeningAktivitetType", opptjeningtype.getKode()));
        };
    }

    public static Inntektskategori mapInntektskategori(no.nav.folketrygdloven.kalkulus.kodeverk.Inntektskategori inntektskategori) {
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

    public static SammenligningsgrunnlagType mapSammenligningsgrunnlagType(no.nav.folketrygdloven.kalkulus.kodeverk.SammenligningsgrunnlagType sammenligningsgrunnlagType) {
        return switch (sammenligningsgrunnlagType) {
            case SAMMENLIGNING_AT -> SammenligningsgrunnlagType.SAMMENLIGNING_AT;
            case SAMMENLIGNING_FL -> SammenligningsgrunnlagType.SAMMENLIGNING_FL;
            case SAMMENLIGNING_AT_FL -> SammenligningsgrunnlagType.SAMMENLIGNING_AT_FL;
            case SAMMENLIGNING_SN -> SammenligningsgrunnlagType.SAMMENLIGNING_SN;
            case SAMMENLIGNING_ATFL_SN -> SammenligningsgrunnlagType.SAMMENLIGNING_ATFL_SN;
            case SAMMENLIGNING_MIDL_INAKTIV -> throw new IllegalArgumentException(ukjentKodeFeil("sammenligningsgrunnlagtype", sammenligningsgrunnlagType.getKode()));
        };
    }

    public static BeregningAktivitetHandlingType mapHandling(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningAktivitetHandlingType handlingType) {
        return switch (handlingType) {
            case BENYTT -> BeregningAktivitetHandlingType.BENYTT;
            case IKKE_BENYTT -> BeregningAktivitetHandlingType.IKKE_BENYTT;
            case UDEFINERT -> BeregningAktivitetHandlingType.UDEFINERT;
        };
    }

    private static String ukjentKodeFeil(String kodeverk, String kode) {
        return String.format("Ikke støttet %s mottatt fra kalkulus, mottok kode %s", kodeverk, kode);
    }

    public static BeregningsgrunnlagTilstand mapTilstand(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        return switch (beregningsgrunnlagTilstand) {
            case OPPRETTET -> BeregningsgrunnlagTilstand.OPPRETTET;
            case FASTSATT_BEREGNINGSAKTIVITETER -> BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER;
            case OPPDATERT_MED_ANDELER -> BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER;
            case KOFAKBER_UT -> BeregningsgrunnlagTilstand.KOFAKBER_UT;
            case BESTEBEREGNET -> BeregningsgrunnlagTilstand.BESTEBEREGNET;
            case FORESLÅTT -> BeregningsgrunnlagTilstand.FORESLÅTT;
            case FORESLÅTT_UT -> BeregningsgrunnlagTilstand.FORESLÅTT_UT;
            case FORESLÅTT_DEL_2 -> BeregningsgrunnlagTilstand.FORESLÅTT_2;
            case FORESLÅTT_DEL_2_UT -> BeregningsgrunnlagTilstand.FORESLÅTT_2_UT;
            case VURDERT_VILKÅR -> BeregningsgrunnlagTilstand.VURDERT_VILKÅR;
            case VURDERT_REFUSJON -> BeregningsgrunnlagTilstand.VURDERT_REFUSJON;
            case VURDERT_REFUSJON_UT -> BeregningsgrunnlagTilstand.VURDERT_REFUSJON_UT;
            case OPPDATERT_MED_REFUSJON_OG_GRADERING -> BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING;
            case FASTSATT_INN -> BeregningsgrunnlagTilstand.FASTSATT_INN;
            case FASTSATT -> BeregningsgrunnlagTilstand.FASTSATT;
            case UDEFINERT -> BeregningsgrunnlagTilstand.UDEFINERT;
            case VURDERT_TILKOMMET_INNTEKT, VURDERT_TILKOMMET_INNTEKT_UT -> throw new IllegalArgumentException(ukjentKodeFeil("BeregningsgrunnlagTilstand", beregningsgrunnlagTilstand.getKode()));
        };
    }

    public static AndelKilde mapKilde(no.nav.folketrygdloven.kalkulus.kodeverk.AndelKilde kilde) {
        return switch (kilde) {
            case PROSESS_START -> AndelKilde.PROSESS_START;
            case SAKSBEHANDLER_KOFAKBER -> AndelKilde.SAKSBEHANDLER_KOFAKBER;
            case PROSESS_BESTEBEREGNING -> AndelKilde.PROSESS_BESTEBEREGNING;
            case SAKSBEHANDLER_FORDELING -> AndelKilde.SAKSBEHANDLER_FORDELING;
            case PROSESS_PERIODISERING -> AndelKilde.PROSESS_PERIODISERING;
            case PROSESS_OMFORDELING -> AndelKilde.PROSESS_OMFORDELING;
            case PROSESS_PERIODISERING_TILKOMMET_INNTEKT -> throw new IllegalStateException(ukjentKodeFeil("AndelKilde", kilde.getKode()));
        };
    }

    public static Hjemmel mapHjemmel(no.nav.folketrygdloven.kalkulus.kodeverk.Hjemmel hjemmel) {
        return switch (hjemmel) {
            case F_14_7 -> Hjemmel.F_14_7;
            case F_14_7_8_30 -> Hjemmel.F_14_7_8_30;
            case F_14_7_8_28_8_30 -> Hjemmel.F_14_7_8_28_8_30;
            case F_14_7_8_35 -> Hjemmel.F_14_7_8_35;
            case F_14_7_8_38 -> Hjemmel.F_14_7_8_38;
            case F_14_7_8_40 -> Hjemmel.F_14_7_8_40;
            case F_14_7_8_41 -> Hjemmel.F_14_7_8_41;
            case F_14_7_8_42 -> Hjemmel.F_14_7_8_42;
            case F_14_7_8_43 -> Hjemmel.F_14_7_8_43;
            case F_14_7_8_47 -> Hjemmel.F_14_7_8_47;
            case F_14_7_8_49 -> Hjemmel.F_14_7_8_49;
            case UDEFINERT -> Hjemmel.UDEFINERT;
            case F_9_9, COV_1_5, F_9_9_8_35, F_9_9_8_38, F_9_9_8_41, F_9_9_8_42, F_9_9_8_43, F_9_9_8_47, F_9_9_8_49, F_9_9_8_28_8_30, F_9_8_8_28,
                 F_9_9_8_40, KORONALOVEN_3, F_22_13_6 -> throw new IllegalArgumentException(ukjentKodeFeil("hjemmel", hjemmel.getKode()));
        };
    }

    public static BeregningsgrunnlagRegelType mapRegelGrunnlagType(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagRegelType type) {
        return switch (type) {
            case SKJÆRINGSTIDSPUNKT -> BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT;
            case BRUKERS_STATUS -> BeregningsgrunnlagRegelType.BRUKERS_STATUS;
            case PERIODISERING_NATURALYTELSE -> BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE;
            case PERIODISERING_REFUSJON -> BeregningsgrunnlagRegelType.PERIODISERING_REFUSJON;
            case PERIODISERING_GRADERING -> BeregningsgrunnlagRegelType.PERIODISERING_GRADERING;
            case UDEFINERT -> BeregningsgrunnlagRegelType.UDEFINERT;
            case PERIODISERING -> BeregningsgrunnlagRegelType.PERIODISERING;
            case PERIODISERING_UTBETALINGSGRAD, BESTEBEREGNING -> throw new IllegalArgumentException(ukjentKodeFeil("regelgrunnlagType", type.getKode()));
        };
    }

    public static BeregningsgrunnlagPeriodeRegelType mapRegelPeriodeType(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagPeriodeRegelType type) {
        return switch (type) {
            case FORESLÅ -> BeregningsgrunnlagPeriodeRegelType.FORESLÅ;
            case FORESLÅ_2 -> BeregningsgrunnlagPeriodeRegelType.FORESLÅ_2;
            case VILKÅR_VURDERING -> BeregningsgrunnlagPeriodeRegelType.VILKÅR_VURDERING;
            case FORDEL -> BeregningsgrunnlagPeriodeRegelType.FORDEL;
            case FASTSETT -> BeregningsgrunnlagPeriodeRegelType.FASTSETT;
            case FINN_GRENSEVERDI -> BeregningsgrunnlagPeriodeRegelType.FINN_GRENSEVERDI;
            case UDEFINERT -> BeregningsgrunnlagPeriodeRegelType.UDEFINERT;
            case OPPDATER_GRUNNLAG_SVP -> BeregningsgrunnlagPeriodeRegelType.OPPDATER_GRUNNLAG_SVP;
            case FASTSETT2 -> BeregningsgrunnlagPeriodeRegelType.FASTSETT2;
        };
    }
}
