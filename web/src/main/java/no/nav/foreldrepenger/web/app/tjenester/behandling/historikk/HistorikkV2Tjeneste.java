package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Tekstlinje;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagDokumentLinkDto;

@ApplicationScoped
public class HistorikkV2Tjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(HistorikkV2Tjeneste.class);

    private HistorikkRepository historikkRepository;
    private Historikkinnslag2Repository historikkinnslag2Repository;
    private BehandlingRepository behandlingRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    @Inject
    public HistorikkV2Tjeneste(HistorikkRepository historikkRepository,
                               Historikkinnslag2Repository historikkinnslag2Repository,
                               BehandlingRepository behandlingRepository,
                               DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.historikkRepository = historikkRepository;
        this.historikkinnslag2Repository = historikkinnslag2Repository;
        this.behandlingRepository = behandlingRepository;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    HistorikkV2Tjeneste() {
        //CDI
    }

    public List<HistorikkinnslagDtoV2> hentForSak(Saksnummer saksnummer, URI dokumentPath) {
        try {
            var journalPosterForSak = dokumentArkivTjeneste.hentAlleJournalposterForSakCached(saksnummer)
                .stream()
                .map(ArkivJournalPost::getJournalpostId)
                .toList();
            var historikkV1 = historikkRepository.hentHistorikkForSaksnummer(saksnummer).stream().map(h -> map(dokumentPath, h, journalPosterForSak));
            var historikkV2 = historikkinnslag2Repository.hent(saksnummer).stream().map(this::map);

            return Stream.concat(historikkV1, historikkV2).sorted(Comparator.comparing(HistorikkinnslagDtoV2::opprettetTidspunkt)).toList();
        } catch (Exception e) {
            LOG.info("Ny historikktjeneste feilet", e);
            return List.of();
        }
    }

    private HistorikkinnslagDtoV2 map(URI dokumentPath, Historikkinnslag h, List<JournalpostId> journalPosterForSak) {
        var behandlingId = h.getBehandlingId();
        var uuid = behandlingId == null ? null : behandlingRepository.hentBehandling(behandlingId).getUuid();
        return HistorikkV2Adapter.map(h, uuid, journalPosterForSak, dokumentPath);
    }

    private HistorikkinnslagDtoV2 map(Historikkinnslag2 h) {
        var behandlingId = h.getBehandlingId();
        var uuid = behandlingId == null ? null : behandlingRepository.hentBehandling(behandlingId).getUuid();
        List<HistorikkInnslagDokumentLinkDto> dokumenter = List.of(); //TODO
        var tekstlinjer = h.getTekstlinjer()
            .stream()
            .sorted(Comparator.comparing(Historikkinnslag2Tekstlinje::getRekkefølgeIndeks))
            .map(Historikkinnslag2Tekstlinje::getTekst)
            .toList();
        return new HistorikkinnslagDtoV2(uuid, HistorikkinnslagDtoV2.HistorikkAktørDto.fra(h.getAktør(), h.getOpprettetAv()), h.getSkjermlenke(),
            h.getOpprettetTidspunkt(), dokumenter, h.getTittel(), tekstlinjer);
    }

}
