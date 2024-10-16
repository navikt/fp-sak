package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class HistorikkDtoTjeneste {

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
            .stream().map(historikkinnslag -> HistorikkV2Adapter.map(historikkinnslag, behandlingRepository.hentBehandling(historikkinnslag.getBehandlingId()).getUuid()))
            .toList();
    }

}
