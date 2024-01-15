package no.nav.foreldrepenger.domene.abakus.mapping;

import java.util.Map;

import no.nav.abakus.iaygrunnlag.kodeverk.InntektskildeType;
import no.nav.abakus.iaygrunnlag.kodeverk.InntektsmeldingInnsendingsårsakType;
import no.nav.abakus.iaygrunnlag.kodeverk.NaturalytelseType;
import no.nav.abakus.iaygrunnlag.kodeverk.UtsettelseÅrsakType;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus;
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

    private static final Map<no.nav.abakus.iaygrunnlag.kodeverk.YtelseType, RelatertYtelseType> ABAKUS_YTELSE_TIL_RELATERT_YTELSE = Map.ofEntries(
            Map.entry(no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.DAGPENGER, RelatertYtelseType.DAGPENGER),
            Map.entry(no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.ARBEIDSAVKLARINGSPENGER, RelatertYtelseType.ARBEIDSAVKLARINGSPENGER),
            Map.entry(no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.SYKEPENGER, RelatertYtelseType.SYKEPENGER),
            Map.entry(no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.PLEIEPENGER_SYKT_BARN, RelatertYtelseType.PLEIEPENGER_SYKT_BARN),
            Map.entry(no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.PLEIEPENGER_NÆRSTÅENDE, RelatertYtelseType.PLEIEPENGER_NÆRSTÅENDE),
            Map.entry(no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.OMSORGSPENGER, RelatertYtelseType.OMSORGSPENGER),
            Map.entry(no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.OPPLÆRINGSPENGER, RelatertYtelseType.OPPLÆRINGSPENGER),
            Map.entry(no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.FRISINN, RelatertYtelseType.FRISINN),
            Map.entry(no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.ENGANGSTØNAD, RelatertYtelseType.ENGANGSSTØNAD),
            Map.entry(no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.FORELDREPENGER, RelatertYtelseType.FORELDREPENGER),
            Map.entry(no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.SVANGERSKAPSPENGER, RelatertYtelseType.SVANGERSKAPSPENGER),
            Map.entry(no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.ENSLIG_FORSØRGER, RelatertYtelseType.ENSLIG_FORSØRGER));

    private static final Map<RelatertYtelseType, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType> RELATERT_YTELSE_TIL_ABAKUS_YTELSE = Map.ofEntries(
            Map.entry(RelatertYtelseType.DAGPENGER, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.DAGPENGER),
            Map.entry(RelatertYtelseType.ARBEIDSAVKLARINGSPENGER, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.ARBEIDSAVKLARINGSPENGER),
            Map.entry(RelatertYtelseType.SYKEPENGER, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.SYKEPENGER),
            Map.entry(RelatertYtelseType.PLEIEPENGER_SYKT_BARN, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.PLEIEPENGER_SYKT_BARN),
            Map.entry(RelatertYtelseType.PLEIEPENGER_NÆRSTÅENDE, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.PLEIEPENGER_NÆRSTÅENDE),
            Map.entry(RelatertYtelseType.OMSORGSPENGER, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.OMSORGSPENGER),
            Map.entry(RelatertYtelseType.OPPLÆRINGSPENGER, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.OPPLÆRINGSPENGER),
            Map.entry(RelatertYtelseType.FRISINN, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.FRISINN),
            Map.entry(RelatertYtelseType.ENGANGSSTØNAD, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.ENGANGSTØNAD),
            Map.entry(RelatertYtelseType.FORELDREPENGER, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.FORELDREPENGER),
            Map.entry(RelatertYtelseType.SVANGERSKAPSPENGER, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.SVANGERSKAPSPENGER),
            Map.entry(RelatertYtelseType.ENSLIG_FORSØRGER, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.ENSLIG_FORSØRGER));

    private static final Map<FagsakYtelseType, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType> FAGSAKYTELSE_TIL_ABAKUS_YTELSE = Map.ofEntries(
            Map.entry(FagsakYtelseType.ENGANGSTØNAD, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.ENGANGSTØNAD),
            Map.entry(FagsakYtelseType.FORELDREPENGER, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.FORELDREPENGER),
            Map.entry(FagsakYtelseType.SVANGERSKAPSPENGER, no.nav.abakus.iaygrunnlag.kodeverk.YtelseType.SVANGERSKAPSPENGER));

    private KodeverkMapper() {
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.YtelseType mapYtelseTypeTilDto(RelatertYtelseType ytelseType) {
        if (ytelseType == null || RelatertYtelseType.UDEFINERT.equals(ytelseType)) {
            return null;
        }
        return RELATERT_YTELSE_TIL_ABAKUS_YTELSE.get(ytelseType);
    }

    public static no.nav.abakus.iaygrunnlag.kodeverk.YtelseType fraFagsakYtelseType(FagsakYtelseType fagsakYtelseType) {
        return FAGSAKYTELSE_TIL_ABAKUS_YTELSE.get(fagsakYtelseType);
    }

    static RelatertYtelseTilstand getFpsakRelatertYtelseTilstandForAbakusYtelseStatus(YtelseStatus dto) {
        if (dto == null) {
            return null;
        }

        var kode = dto.getKode();
        return switch (kode) {
            case "OPPR" -> RelatertYtelseTilstand.IKKE_STARTET;
            case "UBEH" -> RelatertYtelseTilstand.ÅPEN;
            case "AVSLU" -> RelatertYtelseTilstand.AVSLUTTET;
            case "LOP" -> RelatertYtelseTilstand.LØPENDE;
            default -> throw new IllegalArgumentException("Ukjent YtelseStatus: " + dto);
        };
    }

    static YtelseStatus getAbakusYtelseStatusForFpsakRelatertYtelseTilstand(RelatertYtelseTilstand tilstand) {
        var kode = tilstand.getKode();
        return switch (kode) {
            case "IKKESTARTET" -> YtelseStatus.OPPRETTET;
            case "ÅPEN" -> YtelseStatus.UNDER_BEHANDLING;
            case "AVSLUTTET" -> YtelseStatus.AVSLUTTET;
            case "LØPENDE" -> YtelseStatus.LØPENDE;
            default -> throw new IllegalArgumentException("Ukjent RelatertYtelseTilstand: " + kode);
        };
    }

    static InntektYtelseType mapInntektYtelseTypeTilGrunnlag(no.nav.abakus.iaygrunnlag.kodeverk.InntektYtelseType type) {
        return type != null ? InntektYtelseType.valueOf(type.name()) : null;
    }

    static BekreftetPermisjonStatus getBekreftetPermisjonStatus(no.nav.abakus.iaygrunnlag.kodeverk.BekreftetPermisjonStatus kode) {
        return kode == null || "-".equals(kode.getKode()) ? BekreftetPermisjonStatus.UDEFINERT : BekreftetPermisjonStatus.fraKode(kode.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.BekreftetPermisjonStatus mapBekreftetPermisjonStatus(BekreftetPermisjonStatus status) {
        return status == null || BekreftetPermisjonStatus.UDEFINERT.equals(
            status) ? null : no.nav.abakus.iaygrunnlag.kodeverk.BekreftetPermisjonStatus.fraKode(status.getKode());
    }

    static Fagsystem mapFagsystemFraDto(no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem dto) {
        return dto == null
                ? Fagsystem.UDEFINERT
                : Fagsystem.fraKode(dto.getKode());
    }

    static RelatertYtelseType mapYtelseTypeFraDto(no.nav.abakus.iaygrunnlag.kodeverk.YtelseType ytelseType) {
        if (ytelseType == null) {
            return RelatertYtelseType.UDEFINERT;
        }
        return ABAKUS_YTELSE_TIL_RELATERT_YTELSE.getOrDefault(ytelseType, RelatertYtelseType.UDEFINERT);
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem mapFagsystemTilDto(Fagsystem kode) {
        return kode == null || Fagsystem.UDEFINERT.equals(kode) ? null : no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem.fraKode(kode.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.InntektPeriodeType mapInntektPeriodeTypeTilDto(InntektPeriodeType hyppighet) {
        return hyppighet == null || InntektPeriodeType.UDEFINERT.equals(
            hyppighet) ? null : no.nav.abakus.iaygrunnlag.kodeverk.InntektPeriodeType.fraKode(hyppighet.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.Arbeidskategori mapArbeidskategoriTilDto(Arbeidskategori kode) {
        return kode == null || Arbeidskategori.UDEFINERT.equals(kode) ? null : no.nav.abakus.iaygrunnlag.kodeverk.Arbeidskategori.fraKode(
            kode.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType mapArbeidTypeTilDto(ArbeidType arbeidType) {
        return arbeidType == null || ArbeidType.UDEFINERT.equals(arbeidType) ? null : no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType.fraKode(
            arbeidType.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType mapPermisjonbeskrivelseTypeTilDto(PermisjonsbeskrivelseType kode) {
        return kode == null || PermisjonsbeskrivelseType.UDEFINERT.equals(
            kode) ? null : no.nav.abakus.iaygrunnlag.kodeverk.PermisjonsbeskrivelseType.fraKode(kode.getKode());
    }

    static InntektskildeType mapInntektsKildeTilDto(InntektsKilde kode) {
        return kode == null || InntektsKilde.UDEFINERT.equals(kode) ? null : InntektskildeType.fraKode(kode.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.InntektspostType mapInntektspostTypeTilDto(InntektspostType kode) {
        return kode == null || InntektspostType.UDEFINERT.equals(kode) ? null : no.nav.abakus.iaygrunnlag.kodeverk.InntektspostType.fraKode(
            kode.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.SkatteOgAvgiftsregelType mapSkatteOgAvgiftsregelTilDto(SkatteOgAvgiftsregelType kode) {
        return kode == null || SkatteOgAvgiftsregelType.UDEFINERT.equals(
            kode) ? null : no.nav.abakus.iaygrunnlag.kodeverk.SkatteOgAvgiftsregelType.fraKode(kode.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType mapArbeidsforholdHandlingTypeTilDto(ArbeidsforholdHandlingType kode) {
        return kode == null || ArbeidsforholdHandlingType.UDEFINERT.equals(
            kode) ? null : no.nav.abakus.iaygrunnlag.kodeverk.ArbeidsforholdHandlingType.fraKode(kode.getKode());
    }

    static ArbeidType mapArbeidType(no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType dto) {
        return dto == null
                ? ArbeidType.UDEFINERT
                : ArbeidType.fraKode(dto.getKode());
    }

    static InntektsmeldingInnsendingsårsakType mapInntektsmeldingInnsendingsårsak(
            InntektsmeldingInnsendingsårsak kode) {
        return kode == null || InntektsmeldingInnsendingsårsak.UDEFINERT.equals(kode) ? null : InntektsmeldingInnsendingsårsakType.fraKode(
            kode.getKode());
    }

    static NaturalytelseType mapNaturalYtelseTilDto(NaturalYtelseType kode) {
        return kode == null || NaturalYtelseType.UDEFINERT.equals(kode) ? null : NaturalytelseType.fraKode(kode.getKode());
    }

    static UtsettelseÅrsakType mapUtsettelseÅrsakTilDto(UtsettelseÅrsak kode) {
        return kode == null || UtsettelseÅrsak.UDEFINERT.equals(kode) ? null : UtsettelseÅrsakType.fraKode(kode.getKode());
    }

    static InntektsKilde mapInntektsKildeFraDto(InntektskildeType dto) {
        return dto == null
                ? InntektsKilde.UDEFINERT
                : InntektsKilde.fraKode(dto.getKode());
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
        return dto == null
                ? VirksomhetType.UDEFINERT
                : VirksomhetType.fraKode(dto.getKode());
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
        return dto == null
                ? InntektsmeldingInnsendingsårsak.UDEFINERT
                : InntektsmeldingInnsendingsårsak.fraKode(dto.getKode());
    }

    static UtsettelseÅrsak mapUtsettelseÅrsakFraDto(UtsettelseÅrsakType dto) {
        return dto == null
                ? UtsettelseÅrsak.UDEFINERT
                : UtsettelseÅrsak.fraKode(dto.getKode());
    }

    static no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType mapVirksomhetTypeTilDto(VirksomhetType kode) {
        return
            kode == null || VirksomhetType.UDEFINERT.equals(kode) ? null : no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType.fraKode(kode.getKode());
    }

}
