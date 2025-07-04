package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinje;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkinnslagDto.Linje;

@ApplicationScoped
public class HistorikkTjeneste {

    private HistorikkinnslagRepository historikkinnslagRepository;
    private BehandlingRepository behandlingRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    @Inject
    public HistorikkTjeneste(HistorikkinnslagRepository historikkinnslagRepository,
                             BehandlingRepository behandlingRepository,
                             DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.behandlingRepository = behandlingRepository;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    HistorikkTjeneste() {
        //CDI
    }

    public List<HistorikkinnslagDto> hentForSak(Saksnummer saksnummer) {
        var journalPosterForSak = dokumentArkivTjeneste.hentAlleJournalposterForSakCached(saksnummer)
            .stream()
            .map(ArkivJournalPost::getJournalpostId)
            .toList();

        return historikkinnslagRepository.hent(saksnummer).stream()
            .map(h -> map(h, journalPosterForSak))
            .sorted(Comparator.comparing(HistorikkinnslagDto::opprettetTidspunkt))
            .toList();
    }

    private HistorikkinnslagDto map(Historikkinnslag h, List<JournalpostId> journalPosterForSak) {
        var behandlingId = h.getBehandlingId();
        var uuid = behandlingId == null ? null : behandlingRepository.hentBehandling(behandlingId).getUuid();
        List<HistorikkInnslagDokumentLinkDto> dokumenter = tilDokumentlenker(h.getDokumentLinker(), journalPosterForSak);
        var linjer = h.getLinjer()
            .stream()
            .sorted(Comparator.comparing(HistorikkinnslagLinje::getSekvensNr))
            .map(t -> t.getType() == HistorikkinnslagLinjeType.TEKST ? Linje.tekstlinje(t.getTekst()) : Linje.linjeskift())
            .toList();
        return new HistorikkinnslagDto(uuid, HistorikkinnslagDto.HistorikkAktørDto.fra(h.getAktør(), h.getOpprettetAv()), h.getSkjermlenke(),
            h.getOpprettetTidspunkt(), dokumenter, h.getTittel(), linjer);
    }

    private static List<HistorikkInnslagDokumentLinkDto> tilDokumentlenker(List<HistorikkinnslagDokumentLink> dokumentLinker,
                                                                           List<JournalpostId> journalPosterForSak) {
        if (dokumentLinker == null) {
            return List.of();
        }
        return dokumentLinker.stream().map(d -> tilDokumentlenke(d, journalPosterForSak)) //
            .toList();
    }

    private static HistorikkInnslagDokumentLinkDto tilDokumentlenke(HistorikkinnslagDokumentLink lenke,
                                                                     List<JournalpostId> journalPosterForSak) {
        var erUtgått = aktivJournalPost(lenke.getJournalpostId(), journalPosterForSak);
        var dto = new HistorikkInnslagDokumentLinkDto();
        dto.setTag(erUtgått ? String.format("%s (utgått)", lenke.getLinkTekst()) : lenke.getLinkTekst());
        dto.setUtgått(erUtgått);
        dto.setDokumentId(lenke.getDokumentId());
        dto.setJournalpostId(lenke.getJournalpostId().getVerdi());
        return dto;
    }

    private static boolean aktivJournalPost(JournalpostId journalpostId, List<JournalpostId> journalPosterForSak) {
        return journalPosterForSak.stream().filter(ajp -> Objects.equals(ajp, journalpostId)).findFirst().isEmpty();
    }

}
