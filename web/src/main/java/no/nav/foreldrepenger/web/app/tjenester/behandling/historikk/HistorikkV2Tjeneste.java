package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class HistorikkV2Tjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(HistorikkV2Tjeneste.class);

    private HistorikkRepository historikkRepository;
    private BehandlingRepository behandlingRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    @Inject
    public HistorikkV2Tjeneste(HistorikkRepository historikkRepository,
                               BehandlingRepository behandlingRepository,
                               DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.historikkRepository = historikkRepository;
        this.behandlingRepository = behandlingRepository;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    HistorikkV2Tjeneste() {
        //CDI
    }

    public List<HistorikkinnslagDtoV2> hentForSak(Saksnummer saksnummer, URI dokumentPath) {
        try {
            var historikkinnslag = historikkRepository.hentHistorikkForSaksnummer(saksnummer);
            var journalPosterForSak = dokumentArkivTjeneste.hentAlleJournalposterForSakCached(saksnummer).stream()
                .map(ArkivJournalPost::getJournalpostId)
                .toList();
            return historikkinnslag
                .stream()
                .map(h -> {
                    var behandlingId = h.getBehandlingId();
                    var uuid = behandlingId == null ? null : behandlingRepository.hentBehandling(behandlingId).getUuid();
                    return HistorikkV2Adapter.map(h, uuid, journalPosterForSak, dokumentPath);
                })
                .sorted(Comparator.comparing(HistorikkinnslagDtoV2::opprettetTidspunkt))
                .toList();
        } catch (Exception e) {
            LOG.warn("Ny historikktjeneste feilet", e);
            return List.of();
        }
    }

}
