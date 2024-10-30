package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;

import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class HistorikkDtoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(HistorikkDtoTjeneste.class);

    private HistorikkRepository historikkRepository;
    private BehandlingRepository behandlingRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    @Inject
    public HistorikkDtoTjeneste(HistorikkRepository historikkRepository,
                                BehandlingRepository behandlingRepository,
                                DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.historikkRepository = historikkRepository;
        this.behandlingRepository = behandlingRepository;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    HistorikkDtoTjeneste() {
        //CDI
    }

    public List<HistorikkinnslagDtoV2> hentForSak(Saksnummer saksnummer, URI dokumentPath) {
        var historikkinnslag = historikkRepository.hentHistorikkForSaksnummer(saksnummer);
        var journalPosterForSak = dokumentArkivTjeneste.hentAlleJournalposterForSakCached(saksnummer).stream()
            .map(ArkivJournalPost::getJournalpostId)
            .toList();
        return historikkinnslag
            .stream()
            .map(h -> {
                var behandlingId = h.getBehandlingId();
                var uuid = behandlingId == null ? null : behandlingRepository.hentBehandling(behandlingId).getUuid();
                var mapped = HistorikkV2Adapter.map(h, uuid, journalPosterForSak, dokumentPath);
                if (mapped == null) {
                    LOG.info("historikkv2 Mangler implementasjon for {}", h.getType());
                }
                return mapped;
            })
            .filter(Objects::nonNull) //TODO fjerne nonnull
            .sorted(Comparator.comparing(HistorikkinnslagDtoV2::opprettetTidspunkt))
            .toList();
    }

}
