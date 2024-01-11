package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.AndelKilde;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidType;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidsforholdHandlingType;
import no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.Hjemmel;
import no.nav.folketrygdloven.kalkulus.kodeverk.Inntektskategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektskildeType;
import no.nav.folketrygdloven.kalkulus.kodeverk.NaturalYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulus.kodeverk.PeriodeÅrsak;
import no.nav.folketrygdloven.kalkulus.kodeverk.SammenligningsgrunnlagType;
import no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType;

import no.nav.folketrygdloven.kalkulus.kodeverk.TemaUnderkategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.VirksomhetType;

import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.pdl.IdentInformasjon;

import org.apache.kafka.common.quota.ClientQuotaAlteration;

import java.util.Map;

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
            case ARBEIDSAVKLARING -> OpptjeningAktivitetType.ARBEIDSAVKLARING;
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
            case FRILOPP -> throw new IllegalStateException("Ikke støtte for opptjeningstype FRILOPP i kalkulus");
        };
    }

    public static Hjemmel mapHjemmel(no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel hjemmel) {
        return switch(hjemmel) {
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
            case FRILANSER_OPPDRAGSTAKER_MED_MER -> ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER;
            case LØNN_UNDER_UTDANNING -> ArbeidType.LØNN_UNDER_UTDANNING;
            case MARITIMT_ARBEIDSFORHOLD -> ArbeidType.MARITIMT_ARBEIDSFORHOLD;
            case MILITÆR_ELLER_SIVILTJENESTE -> ArbeidType.MILITÆR_ELLER_SIVILTJENESTE;
            case ORDINÆRT_ARBEIDSFORHOLD -> ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;
            case PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD -> ArbeidType.PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
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
            case AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS -> NaturalYtelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS;
            case LOSJI -> NaturalYtelseType.LOSJI;
            case KOST_DØGN -> NaturalYtelseType.KOST_DØGN;
            case BESØKSREISER_HJEMMET_ANNET -> NaturalYtelseType.BESØKSREISER_HJEMMET_ANNET;
            case KOSTBESPARELSE_I_HJEMMET -> NaturalYtelseType.KOSTBESPARELSE_I_HJEMMET;
            case RENTEFORDEL_LÅN -> NaturalYtelseType.RENTEFORDEL_LÅN;
            case BIL -> NaturalYtelseType.BIL;
            case KOST_DAGER -> NaturalYtelseType.KOST_DAGER;
            case BOLIG -> NaturalYtelseType.BOLIG;
            case SKATTEPLIKTIG_DEL_FORSIKRINGER -> NaturalYtelseType.SKATTEPLIKTIG_DEL_FORSIKRINGER;
            case FRI_TRANSPORT -> NaturalYtelseType.FRI_TRANSPORT;
            case OPSJONER -> NaturalYtelseType.OPSJONER;
            case TILSKUDD_BARNEHAGEPLASS -> NaturalYtelseType.TILSKUDD_BARNEHAGEPLASS;
            case ANNET -> NaturalYtelseType.ANNET;
            case BEDRIFTSBARNEHAGEPLASS -> NaturalYtelseType.BEDRIFTSBARNEHAGEPLASS;
            case YRKEBIL_TJENESTLIGBEHOV_KILOMETER -> NaturalYtelseType.YRKEBIL_TJENESTLIGBEHOV_KILOMETER;
            case YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS -> NaturalYtelseType.YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS;
            case INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING -> NaturalYtelseType.INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING;
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

    public static TemaUnderkategori mapTemaUnderkategori(no.nav.foreldrepenger.behandlingslager.ytelse.TemaUnderkategori behandlingsTema) {
        return switch (behandlingsTema) {
            case UDEFINERT -> TemaUnderkategori.UDEFINERT;
            case FORELDREPENGER -> TemaUnderkategori.FORELDREPENGER;
            case BT -> TemaUnderkategori.BT;
            case FL -> TemaUnderkategori.FL;
            case UT -> TemaUnderkategori.UT;
            case OVERGANGSSTØNAD -> TemaUnderkategori.OVERGANGSSTØNAD;
            case ENGANGSSTONAD_FODSEL -> TemaUnderkategori.ENGANGSSTONAD_FODSEL;
            case FORELDREPENGER_FODSEL -> TemaUnderkategori.FORELDREPENGER_FODSEL;
            case FORELDREPENGER_ADOPSJON -> TemaUnderkategori.FORELDREPENGER_ADOPSJON;
            case FORELDREPENGER_SVANGERSKAPSPENGER -> TemaUnderkategori.FORELDREPENGER_SVANGERSKAPSPENGER;
            case SYKEPENGER_SYKEPENGER -> TemaUnderkategori.SYKEPENGER_SYKEPENGER;
            case PÅRØRENDE_OMSORGSPENGER -> TemaUnderkategori.PÅRØRENDE_OMSORGSPENGER;
            case PÅRØRENDE_OPPLÆRINGSPENGER -> TemaUnderkategori.PÅRØRENDE_OPPLÆRINGSPENGER;
            case PÅRØRENDE_PLEIETRENGENDE_SYKT_BARN -> TemaUnderkategori.PÅRØRENDE_PLEIETRENGENDE_SYKT_BARN;
            case PÅRØRENDE_PLEIETRENGENDE -> TemaUnderkategori.PÅRØRENDE_PLEIEPENGER;
            case PÅRØRENDE_PLEIETRENGENDE_PÅRØRENDE -> TemaUnderkategori.PÅRØRENDE_PLEIETRENGENDE_PÅRØRENDE;
            case PÅRØRENDE_PLEIEPENGER -> TemaUnderkategori.PÅRØRENDE_PLEIEPENGER;
            case ENGANGSSTONAD_ADOPSJON -> TemaUnderkategori.ENGANGSSTONAD_ADOPSJON;
            case SYKEPENGER_FORSIKRINGSRISIKO -> TemaUnderkategori.SYKEPENGER_FORSIKRINGSRISIKO;
            case SYKEPENGER_REISETILSKUDD -> TemaUnderkategori.SYKEPENGER_REISETILSKUDD;
            case SYKEPENGER_UTENLANDSOPPHOLD -> TemaUnderkategori.SYKEPENGER_UTENLANDSOPPHOLD;
            case FORELDREPENGER_FODSEL_UTLAND -> TemaUnderkategori.FORELDREPENGER_FODSEL_UTLAND;
        };
    }

    public static FagsakYtelseType mapRelatertYtelseTilFagsakytelse(RelatertYtelseType relatertYtelseType) {
        return switch (relatertYtelseType) {
            case ENSLIG_FORSØRGER -> FagsakYtelseType.ENSLIG_FORSØRGER;
            case SYKEPENGER -> FagsakYtelseType.SYKEPENGER;
            case SVANGERSKAPSPENGER -> FagsakYtelseType.SVANGERSKAPSPENGER;
            case FORELDREPENGER -> FagsakYtelseType.FORELDREPENGER;
            case ENGANGSSTØNAD -> FagsakYtelseType.ENGANGSTØNAD;
            case PÅRØRENDESYKDOM -> FagsakYtelseType.PÅRØRENDESYKDOM;
            case FRISINN -> FagsakYtelseType.FRISINN;
            case PLEIEPENGER_SYKT_BARN -> FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
            case OMSORGSPENGER -> FagsakYtelseType.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> FagsakYtelseType.OPPLÆRINGSPENGER;
            case ARBEIDSAVKLARINGSPENGER -> FagsakYtelseType.ARBEIDSAVKLARINGSPENGER;
            case DAGPENGER -> FagsakYtelseType.DAGPENGER;
            case UDEFINERT -> FagsakYtelseType.UDEFINERT;
        };
    }
}
