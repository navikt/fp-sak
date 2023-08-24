package no.nav.foreldrepenger.dokumentarkiv;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;

import java.util.Map;

class MapNAVSkjemaDokumentTypeId {

    public static final int GEN_RANK = 90;
    public static final int UDEF_RANK = 99;

    private static final Map<NAVSkjema, DokumentTypeId> BREVKODE_DOKUMENT_TYPE = Map.ofEntries(
            Map.entry(NAVSkjema.SKJEMA_SVANGERSKAPSPENGER, DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER),
            Map.entry(NAVSkjema.SKJEMA_SVANGERSKAPSPENGER_SN, DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER),
            Map.entry(NAVSkjema.SKJEMA_TILRETTELEGGING_B, DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER),
            Map.entry(NAVSkjema.SKJEMA_TILRETTELEGGING_N, DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER),
            Map.entry(NAVSkjema.FORSIDE_SVP_GAMMEL, DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER),
            Map.entry(NAVSkjema.SKJEMA_FORELDREPENGER_ADOPSJON, DokumentTypeId.SØKNAD_FORELDREPENGER_ADOPSJON),
            Map.entry(NAVSkjema.SKJEMA_FORELDREPENGER_FØDSEL, DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL),
            Map.entry(NAVSkjema.SKJEMA_ENGANGSSTØNAD_ADOPSJON, DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON),
            Map.entry(NAVSkjema.SKJEMA_ENGANGSSTØNAD_FØDSEL, DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL),
            Map.entry(NAVSkjema.SKJEMA_FLEKSIBELT_UTTAK, DokumentTypeId.FLEKSIBELT_UTTAK_FORELDREPENGER),
            Map.entry(NAVSkjema.SKJEMA_FORELDREPENGER_ENDRING, DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD),
            Map.entry(NAVSkjema.SKJEMA_KLAGE_DOKUMENT, DokumentTypeId.KLAGE_DOKUMENT),
            Map.entry(NAVSkjema.SKJEMA_INNTEKTSOPPLYSNING_SELVSTENDIG, DokumentTypeId.INNTEKTSOPPLYSNING_SELVSTENDIG),
            Map.entry(NAVSkjema.SKJEMA_INNTEKTSOPPLYSNINGER, DokumentTypeId.INNTEKTSOPPLYSNINGER),
            Map.entry(NAVSkjema.SKJEMA_INNTEKTSMELDING, DokumentTypeId.INNTEKTSMELDING),
            Map.entry(NAVSkjema.SKJEMA_ANNEN_POST, DokumentTypeId.ANNET),
            Map.entry(NAVSkjema.SKJEMAE_KLAGE, DokumentTypeId.KLAGE_ETTERSENDELSE));

    private static final Map<DokumentTypeId, Integer> DOKUMENT_TYPE_RANK = Map.ofEntries(
            Map.entry(DokumentTypeId.INNTEKTSMELDING, 1),
            Map.entry(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL, 2),
            Map.entry(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL, 3),
            Map.entry(DokumentTypeId.FLEKSIBELT_UTTAK_FORELDREPENGER, 4),
            Map.entry(DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER, 5),
            Map.entry(DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD, 6),
            Map.entry(DokumentTypeId.SØKNAD_FORELDREPENGER_ADOPSJON, 7),
            Map.entry(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON, 8),
            Map.entry(DokumentTypeId.KLAGE_DOKUMENT, 9),
            Map.entry(DokumentTypeId.KLAGE_ETTERSENDELSE, 10),
            Map.entry(DokumentTypeId.TILBAKE_UTTALSELSE, 11),
            Map.entry(DokumentTypeId.LEGEERKLÆRING, 20),
            Map.entry(DokumentTypeId.DOK_INNLEGGELSE, 21),
            Map.entry(DokumentTypeId.DOK_HV, 22),
            Map.entry(DokumentTypeId.DOK_NAV_TILTAK, 23),
            Map.entry(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL, 30),
            Map.entry(DokumentTypeId.BEKREFTELSE_VENTET_FØDSELSDATO, 31),
            Map.entry(DokumentTypeId.FØDSELSATTEST, 32),
            Map.entry(DokumentTypeId.DOKUMENTASJON_AV_OMSORGSOVERTAKELSE, 33),
            Map.entry(DokumentTypeId.DOK_FERIE, 40),
            Map.entry(DokumentTypeId.DOK_MORS_UTDANNING_ARBEID_SYKDOM, 41),
            Map.entry(DokumentTypeId.BESKRIVELSE_FUNKSJONSNEDSETTELSE, 42),
            Map.entry(DokumentTypeId.BEKREFTELSE_FRA_ARBEIDSGIVER, 43),
            Map.entry(DokumentTypeId.BEKREFTELSE_FRA_STUDIESTED, 44),
            Map.entry(DokumentTypeId.ANNET, 98),
            Map.entry(DokumentTypeId.UDEFINERT, UDEF_RANK));

    private static final Map<Integer, DokumentTypeId> RANK_DOKUMENT_TYPE = Map.ofEntries(
            Map.entry(1, DokumentTypeId.INNTEKTSMELDING),
            Map.entry(2, DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL),
            Map.entry(3, DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL),
            Map.entry(4, DokumentTypeId.FLEKSIBELT_UTTAK_FORELDREPENGER),
            Map.entry(5, DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER),
            Map.entry(6, DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD),
            Map.entry(7, DokumentTypeId.SØKNAD_FORELDREPENGER_ADOPSJON),
            Map.entry(8, DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON),
            Map.entry(9, DokumentTypeId.KLAGE_DOKUMENT),
            Map.entry(10, DokumentTypeId.KLAGE_ETTERSENDELSE),
            Map.entry(11, DokumentTypeId.TILBAKE_UTTALSELSE),
            Map.entry(20, DokumentTypeId.LEGEERKLÆRING),
            Map.entry(21, DokumentTypeId.DOK_INNLEGGELSE),
            Map.entry(22, DokumentTypeId.DOK_HV),
            Map.entry(23, DokumentTypeId.DOK_NAV_TILTAK),
            Map.entry(30, DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL),
            Map.entry(31, DokumentTypeId.BEKREFTELSE_VENTET_FØDSELSDATO),
            Map.entry(32, DokumentTypeId.FØDSELSATTEST),
            Map.entry(33, DokumentTypeId.DOKUMENTASJON_AV_OMSORGSOVERTAKELSE),
            Map.entry(40, DokumentTypeId.DOK_FERIE),
            Map.entry(41, DokumentTypeId.DOK_MORS_UTDANNING_ARBEID_SYKDOM),
            Map.entry(42, DokumentTypeId.BESKRIVELSE_FUNKSJONSNEDSETTELSE),
            Map.entry(43, DokumentTypeId.BEKREFTELSE_FRA_ARBEIDSGIVER),
            Map.entry(44, DokumentTypeId.BEKREFTELSE_FRA_STUDIESTED),
            Map.entry(98, DokumentTypeId.ANNET),
            Map.entry(UDEF_RANK, DokumentTypeId.UDEFINERT));

    private MapNAVSkjemaDokumentTypeId() {
    }

    static DokumentTypeId mapBrevkode(NAVSkjema brevkode) {
        if (brevkode == null) {
            return DokumentTypeId.UDEFINERT;
        }
        return BREVKODE_DOKUMENT_TYPE.getOrDefault(brevkode, DokumentTypeId.UDEFINERT);
    }

    static int dokumentTypeRank(DokumentTypeId dokumentTypeId) {
        if (dokumentTypeId == null) {
            return DOKUMENT_TYPE_RANK.get(DokumentTypeId.UDEFINERT);
        }
        return DOKUMENT_TYPE_RANK.getOrDefault(dokumentTypeId, GEN_RANK);
    }

    static DokumentTypeId dokumentTypeFromRank(int rank) {
        return RANK_DOKUMENT_TYPE.getOrDefault(rank, DokumentTypeId.UDEFINERT);
    }
}
