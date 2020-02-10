package no.nav.foreldrepenger.historikk.dto;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@ApplicationScoped
public class HistorikkInnslagKonverter {

    @Inject
    public HistorikkInnslagKonverter() {
    }

    public HistorikkinnslagDto mapFra(Historikkinnslag historikkinnslag, List<ArkivJournalPost> journalPosterForSak) {
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

    private List<HistorikkInnslagDokumentLinkDto> mapLenker(List<HistorikkinnslagDokumentLink> lenker, List<ArkivJournalPost> journalPosterForSak) {
        return lenker.stream().map(lenke -> map(lenke, journalPosterForSak)).collect(Collectors.toList());
    }

    private HistorikkInnslagDokumentLinkDto map(HistorikkinnslagDokumentLink lenke, List<ArkivJournalPost> journalPosterForSak) {
        Optional<ArkivJournalPost> aktivJournalPost = aktivJournalPost(lenke.getJournalpostId(), journalPosterForSak);
        HistorikkInnslagDokumentLinkDto dto = new HistorikkInnslagDokumentLinkDto();
        dto.setTag(lenke.getLinkTekst());
        dto.setUtgått(!aktivJournalPost.isPresent());
        dto.setDokumentId(lenke.getDokumentId());
        dto.setJournalpostId(lenke.getJournalpostId().getVerdi());
        return dto;
    }

    private Optional<ArkivJournalPost> aktivJournalPost(JournalpostId journalpostId, List<ArkivJournalPost> journalPosterForSak) {
        return journalPosterForSak.stream().filter(ajp -> Objects.equals(ajp.getJournalpostId(), journalpostId)).findFirst();
    }

    private String medStorBokstav(String opprettetAv) {
        return opprettetAv.substring(0, 1).toUpperCase() + opprettetAv.substring(1);
    }
}
