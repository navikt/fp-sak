package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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

    @Inject
    public HistorikkDtoTjeneste(HistorikkRepository historikkRepository, BehandlingRepository behandlingRepository) {
        this.historikkRepository = historikkRepository;
        this.behandlingRepository = behandlingRepository;
    }

    HistorikkDtoTjeneste() {
        //CDI
    }

    public List<HistorikkinnslagDtoV2> hentForSak(Saksnummer saksnummer) {
        return historikkRepository.hentHistorikkForSaksnummer(saksnummer)
            .stream()
            .map(historikkinnslag -> {
                var behandlingId = historikkinnslag.getBehandlingId();
                var uuid = behandlingId == null ? null : behandlingRepository.hentBehandling(behandlingId).getUuid();
                var mapped = HistorikkV2Adapter.map(historikkinnslag, uuid);
                if (mapped == null) {
                    LOG.info("historikkv2 Mangler implementasjon for {}", historikkinnslag.getType());
                }
                return mapped;
            })
            .filter(Objects::nonNull) //TODO fjerne nonnull
            .sorted(Comparator.comparing(HistorikkinnslagDtoV2::opprettetTidspunkt))
            .toList();
    }

}
