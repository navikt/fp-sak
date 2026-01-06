package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidType;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidsforholdHandlingType;
import no.nav.folketrygdloven.kalkulus.kodeverk.Arbeidskategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektskildeType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektspostType;
import no.nav.folketrygdloven.kalkulus.kodeverk.NaturalYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulus.kodeverk.PermisjonsbeskrivelseType;
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

    public static AktivitetStatus mapAktivitetstatus(no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus aktivitetStatus) {
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

    public static YtelseKilde mapYtelseKilde(Fagsystem kilde) {
        return switch (kilde) {
            case FPSAK -> YtelseKilde.FPSAK;
            case INFOTRYGD -> YtelseKilde.INFOTRYGD;
            case ARENA -> YtelseKilde.ARENA;
            case KELVIN ->  YtelseKilde.KELVIN;
            case K9SAK -> YtelseKilde.K9SAK;
            case VLSP -> YtelseKilde.VLSP;
            case UDEFINERT -> YtelseKilde.UDEFINERT;
            case TPS, AAREGISTERET, ENHETSREGISTERET, GOSYS, MEDL, INNTEKT, JOARK -> null;
        };
    }

    public static PermisjonsbeskrivelseType mapPermisjonbeskrivelsetype(no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType permisjonsbeskrivelseType) {
        return switch (permisjonsbeskrivelseType) {
            case UDEFINERT -> PermisjonsbeskrivelseType.UDEFINERT;
            case PERMISJON -> PermisjonsbeskrivelseType.PERMISJON;
            case UTDANNINGSPERMISJON -> PermisjonsbeskrivelseType.UTDANNINGSPERMISJON;
            case UTDANNINGSPERMISJON_IKKE_LOVFESTET -> PermisjonsbeskrivelseType.UTDANNINGSPERMISJON_IKKE_LOVFESTET;
            case UTDANNINGSPERMISJON_LOVFESTET -> PermisjonsbeskrivelseType.UTDANNINGSPERMISJON_LOVFESTET;
            case VELFERDSPERMISJON -> PermisjonsbeskrivelseType.VELFERDSPERMISJON;
            case ANNEN_PERMISJON_IKKE_LOVFESTET -> PermisjonsbeskrivelseType.ANNEN_PERMISJON_IKKE_LOVFESTET;
            case ANNEN_PERMISJON_LOVFESTET -> PermisjonsbeskrivelseType.ANNEN_PERMISJON_LOVFESTET;
            case PERMISJON_MED_FORELDREPENGER -> PermisjonsbeskrivelseType.PERMISJON_MED_FORELDREPENGER;
            case PERMITTERING -> PermisjonsbeskrivelseType.PERMITTERING;
            case PERMISJON_VED_MILITÆRTJENESTE -> PermisjonsbeskrivelseType.PERMISJON_VED_MILITÆRTJENESTE;
        };
    }

    public static Arbeidskategori mapArbeidskategori(no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori arbeidskategori) {
        return switch (arbeidskategori) {
            case FISKER -> Arbeidskategori.FISKER;
            case ARBEIDSTAKER -> Arbeidskategori.ARBEIDSTAKER;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> Arbeidskategori.SELVSTENDIG_NÆRINGSDRIVENDE;
            case KOMBINASJON_ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE -> Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE;
            case SJØMANN -> Arbeidskategori.SJØMANN;
            case JORDBRUKER -> Arbeidskategori.JORDBRUKER;
            case DAGPENGER -> Arbeidskategori.DAGPENGER;
            case INAKTIV -> Arbeidskategori.INAKTIV;
            case KOMBINASJON_ARBEIDSTAKER_OG_JORDBRUKER -> Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_JORDBRUKER;
            case KOMBINASJON_ARBEIDSTAKER_OG_FISKER -> Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_FISKER;
            case FRILANSER -> Arbeidskategori.FRILANSER;
            case KOMBINASJON_ARBEIDSTAKER_OG_FRILANSER -> Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_FRILANSER;
            case KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER -> Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER;
            case DAGMAMMA -> Arbeidskategori.DAGMAMMA;
            case UGYLDIG -> Arbeidskategori.UGYLDIG;
            case UDEFINERT -> Arbeidskategori.UDEFINERT;
        };
    }
}
