package no.nav.foreldrepenger.domene.abakus.mapping;

import no.nav.abakus.iaygrunnlag.kodeverk.InntektskildeType;
import no.nav.abakus.iaygrunnlag.kodeverk.InntektsmeldingInnsendingsårsakType;
import no.nav.abakus.iaygrunnlag.kodeverk.NaturalytelseType;
import no.nav.abakus.iaygrunnlag.kodeverk.UtsettelseÅrsakType;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektPeriodeType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.SkatteOgAvgiftsregelType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;

public final class KodeverkMapper {

    private KodeverkMapper() {
    }

    public static YtelseType fraFagsakYtelseType(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case ENGANGSTØNAD -> YtelseType.ENGANGSTØNAD;
            case FORELDREPENGER -> YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseType.SVANGERSKAPSPENGER;
            case UDEFINERT -> null;
            case null -> null;
        };
    }

    static RelatertYtelseTilstand getFpsakRelatertYtelseTilstandForAbakusYtelseStatus(YtelseStatus dto) {
        return switch (dto) {
            case OPPRETTET -> RelatertYtelseTilstand.IKKE_STARTET;
            case UNDER_BEHANDLING -> RelatertYtelseTilstand.ÅPEN;
            case AVSLUTTET -> RelatertYtelseTilstand.AVSLUTTET;
            case LØPENDE -> RelatertYtelseTilstand.LØPENDE;
            case null -> null;
        };
    }

    static InntektYtelseType mapInntektYtelseTypeTilGrunnlag(no.nav.abakus.iaygrunnlag.kodeverk.InntektYtelseType type) {
        return type != null ? InntektYtelseType.valueOf(type.name()) : null;
    }

    static BekreftetPermisjonStatus getBekreftetPermisjonStatus(no.nav.abakus.iaygrunnlag.kodeverk.BekreftetPermisjonStatus kode) {
        return switch (kode) {
            case UDEFINERT -> BekreftetPermisjonStatus.UDEFINERT;
            case BRUK_PERMISJON -> BekreftetPermisjonStatus.BRUK_PERMISJON;
            case IKKE_BRUK_PERMISJON -> BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON;
            case UGYLDIGE_PERIODER -> BekreftetPermisjonStatus.UGYLDIGE_PERIODER;
            case null -> BekreftetPermisjonStatus.UDEFINERT;
        };
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.BekreftetPermisjonStatus mapBekreftetPermisjonStatus(BekreftetPermisjonStatus status) {
        return switch (status) {
            case UDEFINERT -> null;
            case BRUK_PERMISJON -> no.nav.abakus.iaygrunnlag.kodeverk.BekreftetPermisjonStatus.BRUK_PERMISJON;
            case IKKE_BRUK_PERMISJON -> no.nav.abakus.iaygrunnlag.kodeverk.BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON;
            case UGYLDIGE_PERIODER -> no.nav.abakus.iaygrunnlag.kodeverk.BekreftetPermisjonStatus.UGYLDIGE_PERIODER;
            case null -> null;
        };
    }

    static Fagsystem mapFagsystemFraDto(no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem dto) {
        return switch (dto) {
            case FPSAK -> Fagsystem.FPSAK;
            case K9SAK -> Fagsystem.K9SAK;
            case VLSP -> Fagsystem.VLSP;
            case INFOTRYGD -> Fagsystem.INFOTRYGD;
            case ARENA -> Fagsystem.ARENA;
            case KELVIN -> Fagsystem.KELVIN;
            case DPSAK -> Fagsystem.DPSAK;
            case AAREGISTERET -> Fagsystem.AAREGISTERET;
            case null -> Fagsystem.UDEFINERT;
        };
    }

    static RelatertYtelseType mapYtelseTypeFraDto(YtelseType ytelseType) {
        return switch (ytelseType) {
            case YtelseType.DAGPENGER -> RelatertYtelseType.DAGPENGER;
            case YtelseType.ARBEIDSAVKLARINGSPENGER -> RelatertYtelseType.ARBEIDSAVKLARINGSPENGER;
            case YtelseType.SYKEPENGER -> RelatertYtelseType.SYKEPENGER;
            case YtelseType.PLEIEPENGER_SYKT_BARN -> RelatertYtelseType.PLEIEPENGER_SYKT_BARN;
            case YtelseType.PLEIEPENGER_NÆRSTÅENDE -> RelatertYtelseType.PLEIEPENGER_NÆRSTÅENDE;
            case YtelseType.OMSORGSPENGER -> RelatertYtelseType.OMSORGSPENGER;
            case YtelseType.OPPLÆRINGSPENGER -> RelatertYtelseType.OPPLÆRINGSPENGER;
            case YtelseType.FRISINN -> RelatertYtelseType.FRISINN;
            case YtelseType.ENGANGSTØNAD -> RelatertYtelseType.ENGANGSTØNAD;
            case YtelseType.FORELDREPENGER -> RelatertYtelseType.FORELDREPENGER;
            case YtelseType.SVANGERSKAPSPENGER -> RelatertYtelseType.SVANGERSKAPSPENGER;
            case null -> null;
        };
    }

    public static ArbeidType mapArbeidType(no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType dto) {
        return switch (dto) {
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
            case null -> null;
        };
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType mapArbeidTypeTilDto(ArbeidType arbeidType) {
        return switch (arbeidType) {
            case ETTERLØNN_SLUTTPAKKE -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.ETTERLØNN_SLUTTPAKKE;
            case FORENKLET_OPPGJØRSORDNING -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.FORENKLET_OPPGJØRSORDNING;
            case FRILANSER -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.FRILANSER;
            case FRILANSER_OPPDRAGSTAKER_MED_MER -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER;
            case LØNN_UNDER_UTDANNING -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.LØNN_UNDER_UTDANNING;
            case MARITIMT_ARBEIDSFORHOLD -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.MARITIMT_ARBEIDSFORHOLD;
            case MILITÆR_ELLER_SIVILTJENESTE -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.MILITÆR_ELLER_SIVILTJENESTE;
            case ORDINÆRT_ARBEIDSFORHOLD -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;
            case PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case UTENLANDSK_ARBEIDSFORHOLD -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.UTENLANDSK_ARBEIDSFORHOLD;
            case VENTELØNN_VARTPENGER -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.VENTELØNN_VARTPENGER;
            case null -> null;
        };
    }

    public static PermisjonsbeskrivelseType mapPermisjonbeskrivelseTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType dto) {
        return switch (dto) {
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
            case null -> null;
        };
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType mapPermisjonbeskrivelseTypeTilDto(PermisjonsbeskrivelseType kode) {
        return switch (kode) {
            case PERMISJON -> no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType.PERMISJON;
            case UTDANNINGSPERMISJON -> no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType.UTDANNINGSPERMISJON;
            case UTDANNINGSPERMISJON_IKKE_LOVFESTET -> no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType.UTDANNINGSPERMISJON_IKKE_LOVFESTET;
            case UTDANNINGSPERMISJON_LOVFESTET -> no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType.UTDANNINGSPERMISJON_LOVFESTET;
            case VELFERDSPERMISJON -> no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType.VELFERDSPERMISJON;
            case ANNEN_PERMISJON_IKKE_LOVFESTET -> no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType.ANNEN_PERMISJON_IKKE_LOVFESTET;
            case ANNEN_PERMISJON_LOVFESTET -> no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType.ANNEN_PERMISJON_LOVFESTET;
            case PERMISJON_MED_FORELDREPENGER -> no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType.PERMISJON_MED_FORELDREPENGER;
            case PERMITTERING -> no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType.PERMITTERING;
            case PERMISJON_VED_MILITÆRTJENESTE -> no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType.PERMISJON_VED_MILITÆRTJENESTE;
            case null -> null;
        };
    }

    static ArbeidsforholdHandlingType mapArbeidsforholdHandlingTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType dto) {
        return switch (dto) {
            case BRUK -> ArbeidsforholdHandlingType.BRUK;
            case NYTT_ARBEIDSFORHOLD -> ArbeidsforholdHandlingType.NYTT_ARBEIDSFORHOLD;
            case BRUK_UTEN_INNTEKTSMELDING -> ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING;
            case IKKE_BRUK -> ArbeidsforholdHandlingType.IKKE_BRUK;
            case SLÅTT_SAMMEN_MED_ANNET -> ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET;
            case LAGT_TIL_AV_SAKSBEHANDLER -> ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER;
            case BASERT_PÅ_INNTEKTSMELDING -> ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING;
            case BRUK_MED_OVERSTYRT_PERIODE -> ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE;
            case INNTEKT_IKKE_MED_I_BG -> ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG;
            case null -> null;
        };
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType mapArbeidsforholdHandlingTypeTilDto(ArbeidsforholdHandlingType kode) {
        return switch (kode) {
            case BRUK -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType.BRUK;
            case NYTT_ARBEIDSFORHOLD -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType.NYTT_ARBEIDSFORHOLD;
            case BRUK_UTEN_INNTEKTSMELDING -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING;
            case IKKE_BRUK -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType.IKKE_BRUK;
            case SLÅTT_SAMMEN_MED_ANNET -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET;
            case LAGT_TIL_AV_SAKSBEHANDLER -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER;
            case BASERT_PÅ_INNTEKTSMELDING -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING;
            case BRUK_MED_OVERSTYRT_PERIODE -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE;
            case INNTEKT_IKKE_MED_I_BG -> no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG;
            case null -> null;
        };
    }

    static InntektsmeldingInnsendingsårsak mapInntektsmeldingInnsendingsårsakFraDto(InntektsmeldingInnsendingsårsakType dto) {
        return switch (dto) {
            case NY -> InntektsmeldingInnsendingsårsak.NY;
            case ENDRING -> InntektsmeldingInnsendingsårsak.ENDRING;
            case null -> null;
        };
    }

    static InntektsmeldingInnsendingsårsakType mapInntektsmeldingInnsendingsårsak(InntektsmeldingInnsendingsårsak kode) {
        return switch (kode) {
            case NY -> InntektsmeldingInnsendingsårsakType.NY;
            case ENDRING -> InntektsmeldingInnsendingsårsakType.ENDRING;
            case null -> null;
        };
    }

    static NaturalYtelseType mapNaturalYtelseFraDto(NaturalytelseType dto) {
        return dto == null ? null : NaturalYtelseType.fraKode(dto.getKode());
    }

    static NaturalytelseType mapNaturalYtelseTilDto(NaturalYtelseType kode) {
        return kode == null  ? null : NaturalytelseType.fraKode(kode.getKode());
    }

    static UtsettelseÅrsak mapUtsettelseÅrsakFraDto(UtsettelseÅrsakType dto) {
        return switch (dto) {
            case ARBEID -> UtsettelseÅrsak.ARBEID;
            case FERIE -> UtsettelseÅrsak.FERIE;
            case SYKDOM -> UtsettelseÅrsak.SYKDOM;
            case INSTITUSJON_SØKER -> UtsettelseÅrsak.INSTITUSJON_SØKER;
            case INSTITUSJON_BARN -> UtsettelseÅrsak.INSTITUSJON_BARN;
            case null -> UtsettelseÅrsak.UDEFINERT;
        };
    }

    static UtsettelseÅrsakType mapUtsettelseÅrsakTilDto(UtsettelseÅrsak kode) {
        return switch (kode) {
            case ARBEID -> UtsettelseÅrsakType.ARBEID;
            case FERIE -> UtsettelseÅrsakType.FERIE;
            case SYKDOM -> UtsettelseÅrsakType.SYKDOM;
            case INSTITUSJON_SØKER -> UtsettelseÅrsakType.INSTITUSJON_SØKER;
            case INSTITUSJON_BARN -> UtsettelseÅrsakType.INSTITUSJON_BARN;
            case null, default -> null;
        };
    }

    static InntektsKilde mapInntektsKildeFraDto(InntektskildeType dto) {
        return switch (dto) {
            case INNTEKT_OPPTJENING -> InntektsKilde.INNTEKT_OPPTJENING;
            case INNTEKT_BEREGNING -> InntektsKilde.INNTEKT_BEREGNING;
            case INNTEKT_SAMMENLIGNING -> InntektsKilde.INNTEKT_SAMMENLIGNING;
            case SIGRUN -> InntektsKilde.SIGRUN;
            case null -> null;
        };
    }

    static VirksomhetType mapVirksomhetTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType dto) {
        return switch (dto) {
            case DAGMAMMA -> VirksomhetType.DAGMAMMA;
            case FISKE -> VirksomhetType.FISKE;
            case JORDBRUK_SKOGBRUK -> VirksomhetType.JORDBRUK_SKOGBRUK;
            case ANNEN -> VirksomhetType.ANNEN;
            case null -> null;
        };
    }

    static SkatteOgAvgiftsregelType mapSkatteOgAvgiftsregelFraDto(no.nav.abakus.iaygrunnlag.kodeverk.SkatteOgAvgiftsregelType dto) {
        return switch (dto) {
            case SÆRSKILT_FRADRAG_FOR_SJØFOLK -> SkatteOgAvgiftsregelType.SÆRSKILT_FRADRAG_FOR_SJØFOLK;
            case SVALBARD -> SkatteOgAvgiftsregelType.SVALBARD;
            case SKATTEFRI_ORGANISASJON -> SkatteOgAvgiftsregelType.SKATTEFRI_ORGANISASJON;
            case NETTOLØNN_FOR_SJØFOLK -> SkatteOgAvgiftsregelType.NETTOLØNN_FOR_SJØFOLK;
            case NETTOLØNN -> SkatteOgAvgiftsregelType.NETTOLØNN;
            case KILDESKATT_PÅ_PENSJONER -> SkatteOgAvgiftsregelType.KILDESKATT_PÅ_PENSJONER;
            case JAN_MAYEN_OG_BILANDENE -> SkatteOgAvgiftsregelType.JAN_MAYEN_OG_BILANDENE;
            case UDEFINERT -> SkatteOgAvgiftsregelType.UDEFINERT;
            case null -> SkatteOgAvgiftsregelType.UDEFINERT;
        };
    }

    static InntektspostType mapInntektspostTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.InntektspostType dto) {
        return switch (dto) {
            case LØNN -> InntektspostType.LØNN;
            case YTELSE -> InntektspostType.YTELSE;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> InntektspostType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case NÆRING_FISKE_FANGST_FAMBARNEHAGE -> InntektspostType.NÆRING_FISKE_FANGST_FAMBARNEHAGE;
            case null -> null;
        };
    }

    static Arbeidskategori mapArbeidskategoriFraDto(no.nav.abakus.iaygrunnlag.kodeverk.Arbeidskategori dto) {
        return dto == null ? Arbeidskategori.UDEFINERT : Arbeidskategori.fraKode(dto.getKode());
    }

    static InntektPeriodeType mapInntektPeriodeTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.InntektPeriodeType dto) {
        return switch (dto) {
            case DAGLIG -> InntektPeriodeType.DAGLIG;
            case UKENTLIG -> InntektPeriodeType.UKENTLIG;
            case BIUKENTLIG -> InntektPeriodeType.BIUKENTLIG;
            case MÅNEDLIG -> InntektPeriodeType.MÅNEDLIG;
            case ÅRLIG -> InntektPeriodeType.ÅRLIG;
            case FASTSATT25PAVVIK -> InntektPeriodeType.FASTSATT25PAVVIK;
            case PREMIEGRUNNLAG -> InntektPeriodeType.PREMIEGRUNNLAG;
            case UDEFINERT -> InntektPeriodeType.UDEFINERT;
            case null -> InntektPeriodeType.UDEFINERT;
        };
    }





    static no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType mapVirksomhetTypeTilDto(VirksomhetType kode) {
        return switch (kode) {
            case DAGMAMMA -> no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType.DAGMAMMA;
            case FISKE -> no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType.FISKE;
            case JORDBRUK_SKOGBRUK -> no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType.JORDBRUK_SKOGBRUK;
            case ANNEN -> no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType.ANNEN;
            case null -> null;
        };
    }

}
