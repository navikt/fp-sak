package no.nav.foreldrepenger.historikk.dto;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

public final class HistorikkInnslagKonverter {

    private HistorikkInnslagKonverter() {
    }

    public static HistorikkinnslagDto mapFra(Historikkinnslag historikkinnslag, List<JournalpostId> journalPosterForSak) {
        HistorikkinnslagDto dto = new HistorikkinnslagDto();
        dto.setBehandlingId(historikkinnslag.getBehandlingId());
        List<HistorikkinnslagDelDto> historikkinnslagDeler = HistorikkinnslagDelDto.mapFra(historikkinnslag.getHistorikkinnslagDeler());
        dto.setHistorikkinnslagDeler(historikkinnslagDeler);
        List<HistorikkInnslagDokumentLinkDto> dokumentLinks = mapLenker(historikkinnslag.getDokumentLinker(), journalPosterForSak);
        dto.setDokumentLinks(dokumentLinks);
        if (historikkinnslag.getOpprettetAv() != null) {
            dto.setOpprettetAv(medStorBokstav(historikkinnslag.getOpprettetAv()));
        }
        dto.setOpprettetTidspunkt(historikkinnslag.getOpprettetTidspunkt());
        dto.setType(historikkinnslag.getType());
        dto.setAktoer(historikkinnslag.getAktør());
        dto.setKjoenn(historikkinnslag.getKjoenn());
        return dto;
    }

    private static List<HistorikkInnslagDokumentLinkDto> mapLenker(List<HistorikkinnslagDokumentLink> lenker,
            List<JournalpostId> journalPosterForSak) {
        return lenker.stream().map(lenke -> map(lenke, journalPosterForSak)).collect(Collectors.toList());
    }

    private static HistorikkInnslagDokumentLinkDto map(HistorikkinnslagDokumentLink lenke, List<JournalpostId> journalPosterForSak) {
        Optional<JournalpostId> aktivJournalPost = aktivJournalPost(lenke.getJournalpostId(), journalPosterForSak);
        HistorikkInnslagDokumentLinkDto dto = new HistorikkInnslagDokumentLinkDto();
        dto.setTag(lenke.getLinkTekst());
        dto.setUtgått(aktivJournalPost.isEmpty());
        dto.setDokumentId(lenke.getDokumentId());
        dto.setJournalpostId(lenke.getJournalpostId().getVerdi());
        return dto;
    }

    private static Optional<JournalpostId> aktivJournalPost(JournalpostId journalpostId, List<JournalpostId> journalPosterForSak) {
        return journalPosterForSak.stream().filter(ajp -> Objects.equals(ajp, journalpostId)).findFirst();
    }

    private static String medStorBokstav(String opprettetAv) {
        return opprettetAv.substring(0, 1).toUpperCase() + opprettetAv.substring(1);
    }
}
