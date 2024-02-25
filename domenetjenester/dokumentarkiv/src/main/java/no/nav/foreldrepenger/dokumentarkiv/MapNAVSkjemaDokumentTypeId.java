package no.nav.foreldrepenger.dokumentarkiv;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;

class MapNAVSkjemaDokumentTypeId {

    private static final int MAX_RANK = 99;

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
            Map.entry(DokumentTypeId.TILBAKEBETALING_UTTALSELSE, 11),
            Map.entry(DokumentTypeId.TILBAKEKREVING_UTTALSELSE, 12));

    private static final Map<Integer, DokumentTypeId> RANK_DOKUMENT_TYPE = DOKUMENT_TYPE_RANK.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    private MapNAVSkjemaDokumentTypeId() {
    }

    static DokumentTypeId mapBrevkode(NAVSkjema brevkode) {
        if (brevkode == null) {
            return DokumentTypeId.UDEFINERT;
        }
        return BREVKODE_DOKUMENT_TYPE.getOrDefault(brevkode, DokumentTypeId.UDEFINERT);
    }

    public static DokumentTypeId velgRangertHovedDokumentType(Collection<DokumentTypeId> alleTyper) {
        var typerMedBeskrivelse = alleTyper.stream()
            .filter(t -> !t.erAnnenDokType() && !t.erEttersendelseType())
            .collect(Collectors.toSet());
        var minrank = typerMedBeskrivelse.stream()
            .map(MapNAVSkjemaDokumentTypeId::dokumentTypeRank)
            .min(Comparator.naturalOrder()).orElse(MAX_RANK);
        if (minrank < MAX_RANK) {
            return RANK_DOKUMENT_TYPE.get(minrank);
        } else {
            return typerMedBeskrivelse.stream().findFirst().or(() -> alleTyper.stream().findFirst()).orElse(DokumentTypeId.UDEFINERT);
        }
    }

    private static int dokumentTypeRank(DokumentTypeId dokumentTypeId) {
        return DOKUMENT_TYPE_RANK.getOrDefault(dokumentTypeId, MAX_RANK);
    }
}
