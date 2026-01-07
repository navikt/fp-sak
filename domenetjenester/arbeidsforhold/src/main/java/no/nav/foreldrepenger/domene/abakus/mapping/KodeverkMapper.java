package no.nav.foreldrepenger.domene.abakus.mapping;

import java.util.Map;

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

    private static final Map<YtelseType, RelatertYtelseType> ABAKUS_YTELSE_TIL_RELATERT_YTELSE = Map.ofEntries(
            Map.entry(YtelseType.DAGPENGER, RelatertYtelseType.DAGPENGER),
            Map.entry(YtelseType.ARBEIDSAVKLARINGSPENGER, RelatertYtelseType.ARBEIDSAVKLARINGSPENGER),
            Map.entry(YtelseType.SYKEPENGER, RelatertYtelseType.SYKEPENGER),
            Map.entry(YtelseType.PLEIEPENGER_SYKT_BARN, RelatertYtelseType.PLEIEPENGER_SYKT_BARN),
            Map.entry(YtelseType.PLEIEPENGER_NÆRSTÅENDE, RelatertYtelseType.PLEIEPENGER_NÆRSTÅENDE),
            Map.entry(YtelseType.OMSORGSPENGER, RelatertYtelseType.OMSORGSPENGER),
            Map.entry(YtelseType.OPPLÆRINGSPENGER, RelatertYtelseType.OPPLÆRINGSPENGER),
            Map.entry(YtelseType.FRISINN, RelatertYtelseType.FRISINN),
            Map.entry(YtelseType.ENGANGSTØNAD, RelatertYtelseType.ENGANGSTØNAD),
            Map.entry(YtelseType.FORELDREPENGER, RelatertYtelseType.FORELDREPENGER),
            Map.entry(YtelseType.SVANGERSKAPSPENGER, RelatertYtelseType.SVANGERSKAPSPENGER),
            Map.entry(YtelseType.ENSLIG_FORSØRGER, RelatertYtelseType.ENSLIG_FORSØRGER));

    private static final Map<FagsakYtelseType, YtelseType> FAGSAKYTELSE_TIL_ABAKUS_YTELSE = Map.ofEntries(
            Map.entry(FagsakYtelseType.ENGANGSTØNAD, YtelseType.ENGANGSTØNAD),
            Map.entry(FagsakYtelseType.FORELDREPENGER, YtelseType.FORELDREPENGER),
            Map.entry(FagsakYtelseType.SVANGERSKAPSPENGER, YtelseType.SVANGERSKAPSPENGER));

    private KodeverkMapper() {
    }

    public static YtelseType fraFagsakYtelseType(FagsakYtelseType fagsakYtelseType) {
        return FAGSAKYTELSE_TIL_ABAKUS_YTELSE.get(fagsakYtelseType);
    }

    static RelatertYtelseTilstand getFpsakRelatertYtelseTilstandForAbakusYtelseStatus(YtelseStatus dto) {
        if (dto == null) {
            return null;
        }

        return switch (dto) {
            case OPPRETTET -> RelatertYtelseTilstand.IKKE_STARTET;
            case UNDER_BEHANDLING -> RelatertYtelseTilstand.ÅPEN;
            case AVSLUTTET -> RelatertYtelseTilstand.AVSLUTTET;
            case LØPENDE -> RelatertYtelseTilstand.LØPENDE;
            default -> throw new IllegalArgumentException("Ukjent YtelseStatus: " + dto);
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
            case TPS -> Fagsystem.TPS;
            case JOARK -> Fagsystem.JOARK;
            case INFOTRYGD -> Fagsystem.INFOTRYGD;
            case ARENA -> Fagsystem.ARENA;
            case KELVIN -> Fagsystem.KELVIN;
            case INNTEKT -> Fagsystem.INNTEKT;
            case MEDL -> Fagsystem.MEDL;
            case GOSYS -> Fagsystem.GOSYS;
            case ENHETSREGISTERET -> Fagsystem.ENHETSREGISTERET;
            case AAREGISTERET -> Fagsystem.AAREGISTERET;
            case null, default -> Fagsystem.UDEFINERT;
        };
    }

    static RelatertYtelseType mapYtelseTypeFraDto(YtelseType ytelseType) {
        if (ytelseType == null) {
            return RelatertYtelseType.UDEFINERT;
        }
        return ABAKUS_YTELSE_TIL_RELATERT_YTELSE.getOrDefault(ytelseType, RelatertYtelseType.UDEFINERT);
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType mapArbeidTypeTilDto(ArbeidType arbeidType) {
        return arbeidType == null ? null : no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.fraKode(arbeidType.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType mapPermisjonbeskrivelseTypeTilDto(PermisjonsbeskrivelseType kode) {
        return kode == null || PermisjonsbeskrivelseType.UDEFINERT.equals(
            kode) ? null : no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType.fraKode(kode.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType mapArbeidsforholdHandlingTypeTilDto(ArbeidsforholdHandlingType kode) {
        return kode == null || ArbeidsforholdHandlingType.UDEFINERT.equals(
            kode) ? null : no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType.fraKode(kode.getKode());
    }

    static ArbeidType mapArbeidType(no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType dto) {
        return dto == null ? null : ArbeidType.fraKode(dto.getKode());
    }

    static InntektsmeldingInnsendingsårsakType mapInntektsmeldingInnsendingsårsak(InntektsmeldingInnsendingsårsak kode) {
        return switch (kode) {
            case NY -> InntektsmeldingInnsendingsårsakType.NY;
            case ENDRING -> InntektsmeldingInnsendingsårsakType.ENDRING;
            case UDEFINERT -> null;
            case null -> null;
        };
    }

    static NaturalytelseType mapNaturalYtelseTilDto(NaturalYtelseType kode) {
        return kode == null || NaturalYtelseType.UDEFINERT.equals(kode) ? null : NaturalytelseType.fraKode(kode.getKode());
    }

    static UtsettelseÅrsakType mapUtsettelseÅrsakTilDto(UtsettelseÅrsak kode) {
        return kode == null || UtsettelseÅrsak.UDEFINERT.equals(kode) ? null : UtsettelseÅrsakType.fraKode(kode.getKode());
    }

    static InntektsKilde mapInntektsKildeFraDto(InntektskildeType dto) {
        return switch (dto) {
            case UDEFINERT -> InntektsKilde.UDEFINERT;
            case INNTEKT_OPPTJENING -> InntektsKilde.INNTEKT_OPPTJENING;
            case INNTEKT_BEREGNING -> InntektsKilde.INNTEKT_BEREGNING;
            case INNTEKT_SAMMENLIGNING -> InntektsKilde.INNTEKT_SAMMENLIGNING;
            case SIGRUN -> InntektsKilde.SIGRUN;
            case VANLIG -> InntektsKilde.VANLIG;
            case null -> InntektsKilde.UDEFINERT;
        };
    }

    static ArbeidsforholdHandlingType mapArbeidsforholdHandlingTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType dto) {
        return dto == null
                ? ArbeidsforholdHandlingType.UDEFINERT
                : ArbeidsforholdHandlingType.fraKode(dto.getKode());
    }

    static NaturalYtelseType mapNaturalYtelseFraDto(NaturalytelseType dto) {
        return dto == null
                ? NaturalYtelseType.UDEFINERT
                : NaturalYtelseType.fraKode(dto.getKode());
    }

    static VirksomhetType mapVirksomhetTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType dto) {
        return switch (dto) {
            case DAGMAMMA -> VirksomhetType.DAGMAMMA;
            case FISKE -> VirksomhetType.FISKE;
            case JORDBRUK_SKOGBRUK -> VirksomhetType.JORDBRUK_SKOGBRUK;
            case ANNEN -> VirksomhetType.ANNEN;
            case UDEFINERT -> VirksomhetType.UDEFINERT;
            case null -> VirksomhetType.UDEFINERT;
        };
    }

    static SkatteOgAvgiftsregelType mapSkatteOgAvgiftsregelFraDto(no.nav.abakus.iaygrunnlag.kodeverk.SkatteOgAvgiftsregelType dto) {
        return dto == null
                ? SkatteOgAvgiftsregelType.UDEFINERT
                : SkatteOgAvgiftsregelType.fraKode(dto.getKode());
    }

    static InntektspostType mapInntektspostTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.InntektspostType dto) {
        return dto == null
                ? InntektspostType.UDEFINERT
                : InntektspostType.fraKode(dto.getKode());
    }

    static PermisjonsbeskrivelseType mapPermisjonbeskrivelseTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType dto) {
        return dto == null
                ? PermisjonsbeskrivelseType.UDEFINERT
                : PermisjonsbeskrivelseType.fraKode(dto.getKode());
    }

    static Arbeidskategori mapArbeidskategoriFraDto(no.nav.abakus.iaygrunnlag.kodeverk.Arbeidskategori dto) {
        return dto == null
                ? Arbeidskategori.UDEFINERT
                : Arbeidskategori.fraKode(dto.getKode());
    }

    static InntektPeriodeType mapInntektPeriodeTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.InntektPeriodeType dto) {
        return dto == null
                ? InntektPeriodeType.UDEFINERT
                : InntektPeriodeType.fraKode(dto.getKode());
    }

    static InntektsmeldingInnsendingsårsak mapInntektsmeldingInnsendingsårsakFraDto(
            InntektsmeldingInnsendingsårsakType dto) {
        return switch (dto) {
            case NY -> InntektsmeldingInnsendingsårsak.NY;
            case ENDRING -> InntektsmeldingInnsendingsårsak.ENDRING;
            case UDEFINERT -> InntektsmeldingInnsendingsårsak.UDEFINERT;
            case null -> InntektsmeldingInnsendingsårsak.UDEFINERT;
        };
    }

    static UtsettelseÅrsak mapUtsettelseÅrsakFraDto(UtsettelseÅrsakType dto) {
        return dto == null
                ? UtsettelseÅrsak.UDEFINERT
                : UtsettelseÅrsak.fraKode(dto.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType mapVirksomhetTypeTilDto(VirksomhetType kode) {
        return switch (kode) {
            case DAGMAMMA -> no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType.DAGMAMMA;
            case FISKE -> no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType.FISKE;
            case JORDBRUK_SKOGBRUK -> no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType.JORDBRUK_SKOGBRUK;
            case ANNEN -> no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType.ANNEN;
            case FRILANSER, UDEFINERT -> null;
            case null -> null;
        };
    }

}
