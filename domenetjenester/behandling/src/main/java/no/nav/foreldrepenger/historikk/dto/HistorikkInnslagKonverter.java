package no.nav.foreldrepenger.historikk.dto;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

public final class HistorikkInnslagKonverter {

    private HistorikkInnslagKonverter() {
    }

    public static HistorikkinnslagDto mapFra(Historikkinnslag historikkinnslag,
                                             List<JournalpostId> journalPosterForSak,
                                             BehandlingRepository behandlingRepository,
                                             URI dokumentPath) {
        var dto = new HistorikkinnslagDto();
        if (historikkinnslag.getBehandlingId() != null) {
            dto.setBehandlingId(historikkinnslag.getBehandlingId());
            var behandling = behandlingRepository.hentBehandling(historikkinnslag.getBehandlingId());
            dto.setBehandlingUuid(behandling.getUuid());
        }
        var historikkinnslagDeler = HistorikkinnslagDelDto.mapFra(historikkinnslag.getHistorikkinnslagDeler());
        dto.setHistorikkinnslagDeler(historikkinnslagDeler);
        var dokumentLinks = mapLenker(historikkinnslag.getDokumentLinker(), journalPosterForSak, dokumentPath);
        dto.setDokumentLinks(dokumentLinks);
        if (historikkinnslag.getOpprettetAv() != null) {
            dto.setOpprettetAv(medStorBokstav(historikkinnslag.getOpprettetAv()));
        }
        dto.setOpprettetTidspunkt(historikkinnslag.getOpprettetTidspunkt());
        dto.setType(historikkinnslag.getType());
        dto.setAktoer(historikkinnslag.getAktør());
        return dto;
    }

    private static List<HistorikkInnslagDokumentLinkDto> mapLenker(List<HistorikkinnslagDokumentLink> lenker,
                                                                   List<JournalpostId> journalPosterForSak,
                                                                   URI dokumentPath) {
        return lenker.stream().map(lenke -> map(lenke, journalPosterForSak, dokumentPath)).toList();
    }

    private static HistorikkInnslagDokumentLinkDto map(HistorikkinnslagDokumentLink lenke,
                                                       List<JournalpostId> journalPosterForSak,
                                                       URI dokumentPath) {
        var aktivJournalPost = aktivJournalPost(lenke.getJournalpostId(), journalPosterForSak);
        var dto = new HistorikkInnslagDokumentLinkDto();
        dto.setTag(lenke.getLinkTekst());
        dto.setUtgått(aktivJournalPost.isEmpty());
        dto.setDokumentId(lenke.getDokumentId());
        dto.setJournalpostId(lenke.getJournalpostId().getVerdi());
        if (lenke.getJournalpostId().getVerdi() != null && lenke.getDokumentId() != null) {
            var builder = UriBuilder.fromUri(dokumentPath)
                .queryParam("journalpostId", lenke.getJournalpostId().getVerdi())
                .queryParam("dokumentId", lenke.getDokumentId());
            dto.setUrl(builder.build());
        }
        return dto;
    }

    private static Optional<JournalpostId> aktivJournalPost(JournalpostId journalpostId, List<JournalpostId> journalPosterForSak) {
        return journalPosterForSak.stream().filter(ajp -> Objects.equals(ajp, journalpostId)).findFirst();
    }

    private static String medStorBokstav(String opprettetAv) {
        return opprettetAv.substring(0, 1).toUpperCase() + opprettetAv.substring(1);
    }
}
