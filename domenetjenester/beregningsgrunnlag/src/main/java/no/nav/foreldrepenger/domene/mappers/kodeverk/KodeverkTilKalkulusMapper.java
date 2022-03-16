package no.nav.foreldrepenger.domene.mappers.kodeverk;


import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidType;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidsforholdHandlingType;
import no.nav.folketrygdloven.kalkulus.kodeverk.Arbeidskategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektPeriodeType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektskildeType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektspostType;
import no.nav.folketrygdloven.kalkulus.kodeverk.NaturalYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulus.kodeverk.PermisjonsbeskrivelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.TemaUnderkategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;

public class KodeverkTilKalkulusMapper {

    private KodeverkTilKalkulusMapper() {
        // Skjuler default konstruktør
    }

    public static ArbeidsforholdHandlingType mapArbeidsforholdHandlingType(no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType fraFpsak) {
        return switch(fraFpsak) {
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

    public static VirksomhetType mapVirksomhetstype(no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType fraFpsak) {
        return switch(fraFpsak) {
            case DAGMAMMA -> VirksomhetType.DAGMAMMA;
            case FISKE -> VirksomhetType.FISKE;
            case FRILANSER -> VirksomhetType.FRILANSER;
            case JORDBRUK_SKOGBRUK -> VirksomhetType.JORDBRUK_SKOGBRUK;
            case ANNEN -> VirksomhetType.ANNEN;
            case UDEFINERT -> VirksomhetType.UDEFINERT;
        };
    }

    public static NaturalYtelseType mapNaturalytelsetype(no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType fraFpsak) {
        return switch(fraFpsak) {
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

    public static Arbeidskategori mapArbeidskategori(no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori fraFpsak) {
        return switch(fraFpsak) {
            case DAGMAMMA -> Arbeidskategori.DAGMAMMA;
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
            case UDEFINERT -> Arbeidskategori.UDEFINERT;
            case KOMBINASJON_ARBEIDSTAKER_OG_FRILANSER -> Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_FRILANSER;
            case KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER -> Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER;
            case UGYLDIG -> Arbeidskategori.UGYLDIG;
        };
    }

    public static TemaUnderkategori mapTemaUnderkategori(no.nav.foreldrepenger.behandlingslager.ytelse.TemaUnderkategori fraFpsak) {
        return switch(fraFpsak) {
            case FORELDREPENGER -> TemaUnderkategori.FORELDREPENGER;
            case FORELDREPENGER_FODSEL -> TemaUnderkategori.FORELDREPENGER_FODSEL;
            case FORELDREPENGER_ADOPSJON -> TemaUnderkategori.FORELDREPENGER_ADOPSJON;
            case FORELDREPENGER_SVANGERSKAPSPENGER -> TemaUnderkategori.FORELDREPENGER_SVANGERSKAPSPENGER;
            case SYKEPENGER_SYKEPENGER -> TemaUnderkategori.SYKEPENGER_SYKEPENGER;
            case PÅRØRENDE_OMSORGSPENGER -> TemaUnderkategori.PÅRØRENDE_OMSORGSPENGER;
            case PÅRØRENDE_OPPLÆRINGSPENGER -> TemaUnderkategori.PÅRØRENDE_OPPLÆRINGSPENGER;
            case PÅRØRENDE_PLEIETRENGENDE_SYKT_BARN -> TemaUnderkategori.PÅRØRENDE_PLEIETRENGENDE_SYKT_BARN;
            case PÅRØRENDE_PLEIETRENGENDE -> TemaUnderkategori.PÅRØRENDE_PLEIETRENGENDE;
            case PÅRØRENDE_PLEIETRENGENDE_PÅRØRENDE -> TemaUnderkategori.PÅRØRENDE_PLEIETRENGENDE_PÅRØRENDE;
            case PÅRØRENDE_PLEIEPENGER -> TemaUnderkategori.PÅRØRENDE_PLEIEPENGER;
            case SYKEPENGER_FORSIKRINGSRISIKO -> TemaUnderkategori.SYKEPENGER_FORSIKRINGSRISIKO;
            case SYKEPENGER_REISETILSKUDD -> TemaUnderkategori.SYKEPENGER_REISETILSKUDD;
            case SYKEPENGER_UTENLANDSOPPHOLD -> TemaUnderkategori.SYKEPENGER_UTENLANDSOPPHOLD;
            case OVERGANGSSTØNAD -> TemaUnderkategori.OVERGANGSSTØNAD;
            case FORELDREPENGER_FODSEL_UTLAND -> TemaUnderkategori.FORELDREPENGER_FODSEL_UTLAND;
            case ENGANGSSTONAD_ADOPSJON -> TemaUnderkategori.ENGANGSSTONAD_ADOPSJON;
            case ENGANGSSTONAD_FODSEL -> TemaUnderkategori.ENGANGSSTONAD_FODSEL;
            case BT -> TemaUnderkategori.BT;
            case FL -> TemaUnderkategori.FL;
            case UT -> TemaUnderkategori.UT;
            case UDEFINERT -> TemaUnderkategori.UDEFINERT;
        };
    }

    public static InntektskildeType mapInntektskildeType(InntektsKilde fraFpsak) {
        return switch(fraFpsak) {
            case UDEFINERT -> InntektskildeType.UDEFINERT;
            case INNTEKT_OPPTJENING -> InntektskildeType.INNTEKT_OPPTJENING;
            case INNTEKT_BEREGNING -> InntektskildeType.INNTEKT_BEREGNING;
            case INNTEKT_SAMMENLIGNING -> InntektskildeType.INNTEKT_SAMMENLIGNING;
            case SIGRUN -> InntektskildeType.SIGRUN;
            case VANLIG -> InntektskildeType.VANLIG;
        };
    }

    public static InntektspostType mapInntektspostType(no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType fraFpsak) {
        return switch(fraFpsak) {
            case UDEFINERT -> InntektspostType.UDEFINERT;
            case LØNN -> InntektspostType.LØNN;
            case YTELSE -> InntektspostType.YTELSE;
            case VANLIG -> InntektspostType.VANLIG;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> InntektspostType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case NÆRING_FISKE_FANGST_FAMBARNEHAGE -> InntektspostType.NÆRING_FISKE_FANGST_FAMBARNEHAGE;
        };
    }

    public static ArbeidType mapArbeidtype(no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType fraFpsak) {
        return switch(fraFpsak) {
            case UDEFINERT -> ArbeidType.UDEFINERT;
            case VANLIG -> ArbeidType.VANLIG;
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
        };
    }

    public static OpptjeningAktivitetType mapOpptjeningAktivitettype(no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType fraFpsak) {
        return switch(fraFpsak) {
            case UDEFINERT -> OpptjeningAktivitetType.UDEFINERT;
            case ETTERLØNN_SLUTTPAKKE -> OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE;
            case ARBEIDSAVKLARING -> OpptjeningAktivitetType.ARBEIDSAVKLARING;
            case ARBEID -> OpptjeningAktivitetType.ARBEID;
            case DAGPENGER -> OpptjeningAktivitetType.DAGPENGER;
            case FORELDREPENGER -> OpptjeningAktivitetType.FORELDREPENGER;
            case FRILANS, FRILOPP -> OpptjeningAktivitetType.FRILANS;
            case MILITÆR_ELLER_SIVILTJENESTE -> OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE;
            case UTENLANDSK_ARBEIDSFORHOLD -> OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD;
            case VENTELØNN_VARTPENGER -> OpptjeningAktivitetType.VENTELØNN_VARTPENGER;
            case NÆRING -> OpptjeningAktivitetType.NÆRING;
            case OMSORGSPENGER -> OpptjeningAktivitetType.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> OpptjeningAktivitetType.OPPLÆRINGSPENGER;
            case PLEIEPENGER -> OpptjeningAktivitetType.PLEIEPENGER;
            case FRISINN -> OpptjeningAktivitetType.FRISINN;
            case SVANGERSKAPSPENGER -> OpptjeningAktivitetType.SVANGERSKAPSPENGER;
            case SYKEPENGER -> OpptjeningAktivitetType.SYKEPENGER;
            case VIDERE_ETTERUTDANNING -> OpptjeningAktivitetType.VIDERE_ETTERUTDANNING;
            case UTDANNINGSPERMISJON -> OpptjeningAktivitetType.UTDANNINGSPERMISJON;
        };
    }

    public static PermisjonsbeskrivelseType mapPermisjonsbeskrivelsetype(no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType fraFpsak) {
        return switch(fraFpsak) {
            case UDEFINERT -> PermisjonsbeskrivelseType.UDEFINERT;
            case PERMISJON -> PermisjonsbeskrivelseType.PERMISJON;
            case UTDANNINGSPERMISJON -> PermisjonsbeskrivelseType.UTDANNINGSPERMISJON;
            case VELFERDSPERMISJON -> PermisjonsbeskrivelseType.VELFERDSPERMISJON;
            case PERMISJON_MED_FORELDREPENGER -> PermisjonsbeskrivelseType.PERMISJON_MED_FORELDREPENGER;
            case PERMITTERING -> PermisjonsbeskrivelseType.PERMITTERING;
            case PERMISJON_VED_MILITÆRTJENESTE -> PermisjonsbeskrivelseType.PERMISJON_VED_MILITÆRTJENESTE;
        };
    }

    public static InntektPeriodeType mapInntektPeiodetype(no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektPeriodeType fraFpsak) {
        return switch(fraFpsak) {
            case DAGLIG -> InntektPeriodeType.DAGLIG;
            case UKENTLIG -> InntektPeriodeType.UKENTLIG;
            case BIUKENTLIG -> InntektPeriodeType.BIUKENTLIG;
            case MÅNEDLIG -> InntektPeriodeType.MÅNEDLIG;
            case ÅRLIG -> InntektPeriodeType.ÅRLIG;
            case FASTSATT25PAVVIK -> InntektPeriodeType.FASTSATT25PAVVIK;
            case PREMIEGRUNNLAG -> InntektPeriodeType.PREMIEGRUNNLAG;
            case UDEFINERT -> InntektPeriodeType.UDEFINERT;
        };
    }
}
