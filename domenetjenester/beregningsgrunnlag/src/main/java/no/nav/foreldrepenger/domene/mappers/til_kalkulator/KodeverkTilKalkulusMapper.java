package no.nav.foreldrepenger.domene.mappers.til_kalkulator;

import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.AndelKilde;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidType;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidsforholdHandlingType;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningAktivitetHandlingType;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagRegelType;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagPeriodeRegelType;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.folketrygdloven.kalkulus.kodeverk.Hjemmel;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.Inntektskategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektskildeType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektspostType;
import no.nav.folketrygdloven.kalkulus.kodeverk.NaturalYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulus.kodeverk.PeriodeÅrsak;
import no.nav.folketrygdloven.kalkulus.kodeverk.SammenligningsgrunnlagType;
import no.nav.folketrygdloven.kalkulus.kodeverk.SkatteOgAvgiftsregelType;
import no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType;
import no.nav.folketrygdloven.kalkulus.kodeverk.VirksomhetType;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseKilde;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;

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

    public static OpptjeningAktivitetType mapOpptjeningAktivitetType(no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType opptjeningtype) {
        return switch (opptjeningtype) {
            case ARBEIDSAVKLARING -> OpptjeningAktivitetType.AAP;
            case ARBEID -> OpptjeningAktivitetType.ARBEID;
            case ARBEID_UNDER_AAP -> OpptjeningAktivitetType.ARBEID_UNDER_AAP;
            case DAGPENGER -> OpptjeningAktivitetType.DAGPENGER;
            case FORELDREPENGER -> OpptjeningAktivitetType.FORELDREPENGER;
            case FRILANS, FRILOPP -> OpptjeningAktivitetType.FRILANS;
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
        };
    }

    public static Hjemmel mapHjemmel(no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel hjemmel) {
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
        };
    }

    public static PeriodeÅrsak mapPeriodeårsak(no.nav.foreldrepenger.domene.modell.kodeverk.PeriodeÅrsak periodeÅrsak) {
        return switch (periodeÅrsak) {
            case NATURALYTELSE_BORTFALT -> PeriodeÅrsak.NATURALYTELSE_BORTFALT;
            case ARBEIDSFORHOLD_AVSLUTTET -> PeriodeÅrsak.ARBEIDSFORHOLD_AVSLUTTET;
            case NATURALYTELSE_TILKOMMER -> PeriodeÅrsak.NATURALYTELSE_TILKOMMER;
            case ENDRING_I_REFUSJONSKRAV -> PeriodeÅrsak.ENDRING_I_REFUSJONSKRAV;
            case REFUSJON_OPPHØRER -> PeriodeÅrsak.REFUSJON_OPPHØRER;
            case GRADERING -> PeriodeÅrsak.GRADERING;
            case GRADERING_OPPHØRER -> PeriodeÅrsak.GRADERING_OPPHØRER;
            case ENDRING_I_AKTIVITETER_SØKT_FOR -> PeriodeÅrsak.ENDRING_I_AKTIVITETER_SØKT_FOR;
            case REFUSJON_AVSLÅTT -> PeriodeÅrsak.REFUSJON_AVSLÅTT;
            case UDEFINERT -> PeriodeÅrsak.UDEFINERT;
        };
    }

    public static ArbeidType mapArbeidtype(no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType arbeidType) {
        return switch (arbeidType) {
            case ETTERLØNN_SLUTTPAKKE -> ArbeidType.ETTERLØNN_SLUTTPAKKE;
            case FORENKLET_OPPGJØRSORDNING -> ArbeidType.FORENKLET_OPPGJØRSORDNING;
            case FRILANSER -> ArbeidType.FRILANSER;
            case FRILANSER_OPPDRAGSTAKER_MED_MER -> ArbeidType.FRILANSER_OPPDRAGSTAKER;
            case LØNN_UNDER_UTDANNING -> ArbeidType.LØNN_UNDER_UTDANNING;
            case MARITIMT_ARBEIDSFORHOLD -> ArbeidType.MARITIMT_ARBEIDSFORHOLD;
            case MILITÆR_ELLER_SIVILTJENESTE -> ArbeidType.MILITÆR_ELLER_SIVILTJENESTE;
            case ORDINÆRT_ARBEIDSFORHOLD -> ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;
            case PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD -> ArbeidType.PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> ArbeidType.NÆRING;
            case UTENLANDSK_ARBEIDSFORHOLD -> ArbeidType.UTENLANDSK_ARBEIDSFORHOLD;
            case VENTELØNN_VARTPENGER -> ArbeidType.VENTELØNN_VARTPENGER;
            case VANLIG -> ArbeidType.VANLIG;
            case UDEFINERT -> ArbeidType.UDEFINERT;
        };
    }

    public static VirksomhetType mapVirksomhetstype(no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType virksomhetType) {
        return switch (virksomhetType) {
            case DAGMAMMA -> VirksomhetType.DAGMAMMA;
            case FISKE -> VirksomhetType.FISKE;
            case FRILANSER -> VirksomhetType.FRILANSER;
            case JORDBRUK_SKOGBRUK -> VirksomhetType.JORDBRUK_SKOGBRUK;
            case ANNEN -> VirksomhetType.ANNEN;
            case UDEFINERT -> VirksomhetType.UDEFINERT;
        };
    }

    public static ArbeidsforholdHandlingType mapArbeidsforholdHandling(no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType handling) {
        return switch (handling) {
            case UDEFINERT -> ArbeidsforholdHandlingType.UDEFINERT;
            case BRUK -> ArbeidsforholdHandlingType.BRUK;
            case NYTT_ARBEIDSFORHOLD -> ArbeidsforholdHandlingType.NYTT_ARBEIDSFORHOLD;
            case BRUK_UTEN_INNTEKTSMELDING -> ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING;
            case IKKE_BRUK -> ArbeidsforholdHandlingType.IKKE_BRUK;
            case SLÅTT_SAMMEN_MED_ANNET -> ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET;
            case LAGT_TIL_AV_SAKSBEHANDLER -> ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER;
            case BASERT_PÅ_INNTEKTSMELDING -> ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING;
            case BRUK_MED_OVERSTYRT_PERIODE -> ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE;
            case INNTEKT_IKKE_MED_I_BG -> ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG;
        };
    }

    public static NaturalYtelseType mapNaturalytelsetype(no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType type) {
        return switch (type) {
            case ELEKTRISK_KOMMUNIKASJON -> NaturalYtelseType.ELEKTRISK_KOMMUNIKASJON;
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS -> NaturalYtelseType.AKSJER_UNDERKURS;
            case LOSJI -> NaturalYtelseType.LOSJI;
            case KOST_DØGN -> NaturalYtelseType.KOST_DOEGN;
            case BESØKSREISER_HJEMMET_ANNET -> NaturalYtelseType.BESOEKSREISER_HJEM;
            case KOSTBESPARELSE_I_HJEMMET -> NaturalYtelseType.KOSTBESPARELSE_HJEM;
            case RENTEFORDEL_LÅN -> NaturalYtelseType.RENTEFORDEL_LAAN;
            case BIL -> NaturalYtelseType.BIL;
            case KOST_DAGER -> NaturalYtelseType.KOST_DAGER;
            case BOLIG -> NaturalYtelseType.BOLIG;
            case SKATTEPLIKTIG_DEL_FORSIKRINGER -> NaturalYtelseType.FORSIKRINGER;
            case FRI_TRANSPORT -> NaturalYtelseType.FRI_TRANSPORT;
            case OPSJONER -> NaturalYtelseType.OPSJONER;
            case TILSKUDD_BARNEHAGEPLASS -> NaturalYtelseType.TILSKUDD_BARNEHAGE;
            case ANNET -> NaturalYtelseType.ANNET;
            case BEDRIFTSBARNEHAGEPLASS -> NaturalYtelseType.BEDRIFTSBARNEHAGE;
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER -> NaturalYtelseType.YRKESBIL_KILOMETER;
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS -> NaturalYtelseType.YRKESBIL_LISTEPRIS;
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING -> NaturalYtelseType.UTENLANDSK_PENSJONSORDNING;
            case UDEFINERT -> NaturalYtelseType.UDEFINERT;
        };
    }

    public static InntektskildeType mapInntektskilde(InntektsKilde inntektsKilde) {
        return switch (inntektsKilde) {
            case UDEFINERT -> InntektskildeType.UDEFINERT;
            case INNTEKT_OPPTJENING -> InntektskildeType.INNTEKT_OPPTJENING;
            case INNTEKT_BEREGNING -> InntektskildeType.INNTEKT_BEREGNING;
            case INNTEKT_SAMMENLIGNING -> InntektskildeType.INNTEKT_SAMMENLIGNING;
            case SIGRUN -> InntektskildeType.SIGRUN;
            case VANLIG -> InntektskildeType.VANLIG;
        };
    }

    public static FagsakYtelseType mapFagsakytelsetype(no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> FagsakYtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> FagsakYtelseType.SVANGERSKAPSPENGER;
            case ENGANGSTØNAD, UDEFINERT -> throw new IllegalStateException("Skal ikke kalle kalkulus med ytelsetype " + fagsakYtelseType);
        };
    }

    public static FaktaOmBeregningTilfelle mapFaktaBeregningTilfelle(no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle tilfelle) {
        return switch (tilfelle) {
            case VURDER_TIDSBEGRENSET_ARBEIDSFORHOLD -> FaktaOmBeregningTilfelle.VURDER_TIDSBEGRENSET_ARBEIDSFORHOLD;
            case VURDER_SN_NY_I_ARBEIDSLIVET -> FaktaOmBeregningTilfelle.VURDER_SN_NY_I_ARBEIDSLIVET;
            case VURDER_NYOPPSTARTET_FL -> FaktaOmBeregningTilfelle.VURDER_NYOPPSTARTET_FL;
            case FASTSETT_MAANEDSINNTEKT_FL -> FaktaOmBeregningTilfelle.FASTSETT_MAANEDSINNTEKT_FL;
            case FASTSETT_BG_ARBEIDSTAKER_UTEN_INNTEKTSMELDING -> FaktaOmBeregningTilfelle.FASTSETT_BG_ARBEIDSTAKER_UTEN_INNTEKTSMELDING;
            case VURDER_LØNNSENDRING -> FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING;
            case FASTSETT_MÅNEDSLØNN_ARBEIDSTAKER_UTEN_INNTEKTSMELDING ->
                FaktaOmBeregningTilfelle.FASTSETT_MÅNEDSLØNN_ARBEIDSTAKER_UTEN_INNTEKTSMELDING;
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
            case FASTSETT_INNTEKT_FOR_ARBEID_UNDER_AAP -> FaktaOmBeregningTilfelle.FASTSETT_INNTEKT_FOR_ARBEID_UNDER_AAP;
            case UDEFINERT -> FaktaOmBeregningTilfelle.UDEFINERT;
        };
    }

    public static BeregningAktivitetHandlingType mapBeregningAktivitetHandling(no.nav.foreldrepenger.domene.modell.kodeverk.BeregningAktivitetHandlingType handling) {
        return switch (handling) {
            case BENYTT -> BeregningAktivitetHandlingType.BENYTT;
            case IKKE_BENYTT -> BeregningAktivitetHandlingType.IKKE_BENYTT;
            case UDEFINERT -> BeregningAktivitetHandlingType.UDEFINERT;
        };
    }

    public static BeregningsgrunnlagTilstand mapBeregningsgrunnlagTilstand(no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        return switch (beregningsgrunnlagTilstand) {
            case OPPRETTET -> BeregningsgrunnlagTilstand.OPPRETTET;
            case FASTSATT_BEREGNINGSAKTIVITETER -> BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER;
            case OPPDATERT_MED_ANDELER -> BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER;
            case KOFAKBER_UT -> BeregningsgrunnlagTilstand.KOFAKBER_UT;
            case BESTEBEREGNET -> BeregningsgrunnlagTilstand.BESTEBEREGNET;
            case FORESLÅTT -> BeregningsgrunnlagTilstand.FORESLÅTT;
            case FORESLÅTT_UT -> BeregningsgrunnlagTilstand.FORESLÅTT_UT;
            case FORESLÅTT_2 -> BeregningsgrunnlagTilstand.FORESLÅTT_DEL_2;
            case FORESLÅTT_2_UT -> BeregningsgrunnlagTilstand.FORESLÅTT_DEL_2_UT;
            case VURDERT_VILKÅR -> BeregningsgrunnlagTilstand.VURDERT_VILKÅR;
            case VURDERT_REFUSJON -> BeregningsgrunnlagTilstand.VURDERT_REFUSJON;
            case VURDERT_REFUSJON_UT -> BeregningsgrunnlagTilstand.VURDERT_REFUSJON_UT;
            case OPPDATERT_MED_REFUSJON_OG_GRADERING -> BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING;
            case FASTSATT_INN -> BeregningsgrunnlagTilstand.FASTSATT_INN;
            case FASTSATT -> BeregningsgrunnlagTilstand.FASTSATT;
            case UDEFINERT -> BeregningsgrunnlagTilstand.UDEFINERT;
        };
    }

    public static InntektspostType mapInntektspostType(no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType type) {
        return switch (type) {
            case UDEFINERT -> InntektspostType.UDEFINERT;
            case LØNN -> InntektspostType.LØNN;
            case YTELSE -> InntektspostType.YTELSE;
            case VANLIG -> InntektspostType.VANLIG;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> InntektspostType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case NÆRING_FISKE_FANGST_FAMBARNEHAGE -> InntektspostType.NÆRING_FISKE_FANGST_FAMBARNEHAGE;
            case null -> null;
        };
    }
    public static SkatteOgAvgiftsregelType mapSkatteOgAvgitsregelType(no.nav.foreldrepenger.domene.iay.modell.kodeverk.SkatteOgAvgiftsregelType type) {
        return switch (type) {
            case SÆRSKILT_FRADRAG_FOR_SJØFOLK -> SkatteOgAvgiftsregelType.SÆRSKILT_FRADRAG_FOR_SJØFOLK;
            case SVALBARD -> SkatteOgAvgiftsregelType.SVALBARD;
            case SKATTEFRI_ORGANISASJON -> SkatteOgAvgiftsregelType.SKATTEFRI_ORGANISASJON;
            case NETTOLØNN_FOR_SJØFOLK -> SkatteOgAvgiftsregelType.NETTOLØNN_FOR_SJØFOLK;
            case NETTOLØNN -> SkatteOgAvgiftsregelType.NETTOLØNN;
            case KILDESKATT_PÅ_PENSJONER -> SkatteOgAvgiftsregelType.KILDESKATT_PÅ_PENSJONER;
            case JAN_MAYEN_OG_BILANDENE -> SkatteOgAvgiftsregelType.JAN_MAYEN_OG_BILANDENE;
            case UDEFINERT -> SkatteOgAvgiftsregelType.UDEFINERT;
        };
    }

    public static InntektYtelseType mapInntektytelseType(no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektYtelseType inntektYtelseType) {
        return switch (inntektYtelseType) {
            case SVANGERSKAPSPENGER -> InntektYtelseType.SVANGERSKAPSPENGER;
            case FORELDREPENGER -> InntektYtelseType.FORELDREPENGER;
            case SYKEPENGER -> InntektYtelseType.SYKEPENGER;
            case OPPLÆRINGSPENGER -> InntektYtelseType.OPPLÆRINGSPENGER;
            case OMSORGSPENGER -> InntektYtelseType.OMSORGSPENGER;
            case ANNET -> InntektYtelseType.ANNET;
            case AAP -> InntektYtelseType.AAP;
            case VEDERLAG -> InntektYtelseType.VEDERLAG;
            case OVERGANGSSTØNAD_ENSLIG -> InntektYtelseType.OVERGANGSSTØNAD_ENSLIG;
            case VENTELØNN -> InntektYtelseType.VENTELØNN;
            case PLEIEPENGER -> InntektYtelseType.PLEIEPENGER;
            case DAGPENGER -> InntektYtelseType.DAGPENGER;
            case FERIEPENGER_FORELDREPENGER -> InntektYtelseType.FERIEPENGER_FORELDREPENGER;
            case FERIEPENGER_SVANGERSKAPSPENGER -> InntektYtelseType.FERIEPENGER_SVANGERSKAPSPENGER;
            case FERIEPENGER_OMSORGSPENGER -> InntektYtelseType.FERIEPENGER_OMSORGSPENGER;
            case FERIEPENGER_OPPLÆRINGSPENGER -> InntektYtelseType.FERIEPENGER_OPPLÆRINGSPENGER;
            case FERIEPENGER_PLEIEPENGER -> InntektYtelseType.FERIEPENGER_PLEIEPENGER;
            case FERIEPENGER_SYKEPENGER -> InntektYtelseType.FERIEPENGER_SYKEPENGER;
            case FERIETILLEGG_DAGPENGER -> InntektYtelseType.FERIETILLEGG_DAGPENGER;
            case KVALIFISERINGSSTØNAD -> InntektYtelseType.KVALIFISERINGSSTØNAD;
            case FORELDREPENGER_NÆRING -> InntektYtelseType.FORELDREPENGER_NÆRING;
            case SVANGERSKAPSPENGER_NÆRING -> InntektYtelseType.SVANGERSKAPSPENGER_NÆRING;
            case SYKEPENGER_NÆRING -> InntektYtelseType.SYKEPENGER_NÆRING;
            case OMSORGSPENGER_NÆRING -> InntektYtelseType.OMSORGSPENGER_NÆRING;
            case OPPLÆRINGSPENGER_NÆRING -> InntektYtelseType.OPPLÆRINGSPENGER_NÆRING;
            case PLEIEPENGER_NÆRING -> InntektYtelseType.PLEIEPENGER_NÆRING;
            case DAGPENGER_NÆRING -> InntektYtelseType.DAGPENGER_NÆRING;
            case LOTT_KUN_TRYGDEAVGIFT -> InntektYtelseType.LOTT_KUN_TRYGDEAVGIFT;
            case KOMPENSASJON_FOR_TAPT_PERSONINNTEKT -> InntektYtelseType.KOMPENSASJON_FOR_TAPT_PERSONINNTEKT;
        };
    }

    public static YtelseType mapYtelsetype(RelatertYtelseType relatertYtelseType) {
        return switch (relatertYtelseType) {
            case ENSLIG_FORSØRGER -> YtelseType.ENSLIG_FORSØRGER;
            case SYKEPENGER -> YtelseType.SYKEPENGER;
            case SVANGERSKAPSPENGER -> YtelseType.SVANGERSKAPSPENGER;
            case FORELDREPENGER -> YtelseType.FORELDREPENGER;
            case ENGANGSTØNAD -> YtelseType.ENGANGSTØNAD;
            case FRISINN -> YtelseType.FRISINN;
            case PLEIEPENGER_SYKT_BARN -> YtelseType.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> YtelseType.PLEIEPENGER_NÆRSTÅENDE;
            case OMSORGSPENGER -> YtelseType.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> YtelseType.OPPLÆRINGSPENGER;
            case ARBEIDSAVKLARINGSPENGER -> YtelseType.ARBEIDSAVKLARINGSPENGER;
            case DAGPENGER -> YtelseType.DAGPENGER;
            case UDEFINERT -> YtelseType.UDEFINERT;
        };
    }

    public static BeregningsgrunnlagRegelType mapGrunnlagRegeltype(no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType regeltype) {
        return switch (regeltype) {
            case SKJÆRINGSTIDSPUNKT -> BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT;
            case BRUKERS_STATUS -> BeregningsgrunnlagRegelType.BRUKERS_STATUS;
            case PERIODISERING -> BeregningsgrunnlagRegelType.PERIODISERING;
            case PERIODISERING_NATURALYTELSE -> BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE;
            case PERIODISERING_REFUSJON -> BeregningsgrunnlagRegelType.PERIODISERING_REFUSJON;
            case PERIODISERING_GRADERING -> BeregningsgrunnlagRegelType.PERIODISERING_GRADERING;
            case UDEFINERT -> BeregningsgrunnlagRegelType.UDEFINERT;
        };
    }

    public static BeregningsgrunnlagPeriodeRegelType mapPeriodeRegelType(no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagPeriodeRegelType regeltype) {
        return switch (regeltype) {
            case FORESLÅ -> BeregningsgrunnlagPeriodeRegelType.FORESLÅ;
            case FORESLÅ_2 -> BeregningsgrunnlagPeriodeRegelType.FORESLÅ_2;
            case VILKÅR_VURDERING -> BeregningsgrunnlagPeriodeRegelType.VILKÅR_VURDERING;
            case FORDEL -> BeregningsgrunnlagPeriodeRegelType.FORDEL;
            case FASTSETT -> BeregningsgrunnlagPeriodeRegelType.FASTSETT;
            case OPPDATER_GRUNNLAG_SVP -> BeregningsgrunnlagPeriodeRegelType.OPPDATER_GRUNNLAG_SVP;
            case FASTSETT2 -> BeregningsgrunnlagPeriodeRegelType.FASTSETT2;
            case FINN_GRENSEVERDI -> BeregningsgrunnlagPeriodeRegelType.FINN_GRENSEVERDI    ;
            case UDEFINERT -> BeregningsgrunnlagPeriodeRegelType.UDEFINERT;
        };
    }

    public static YtelseKilde mapYtelseKilde(Fagsystem kilde) {
        return switch (kilde) {
            case FPSAK -> YtelseKilde.FPSAK;
            case INFOTRYGD -> YtelseKilde.INFOTRYGD;
            case ARENA -> YtelseKilde.ARENA;
            case K9SAK -> YtelseKilde.K9SAK;
            case VLSP -> YtelseKilde.VLSP;
            case UDEFINERT -> YtelseKilde.UDEFINERT;
            case TPS, AAREGISTERET, ENHETSREGISTERET, GOSYS, MEDL, INNTEKT, JOARK -> null;
        };
    }
}
