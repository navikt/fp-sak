package no.nav.foreldrepenger.behandlingsprosess.prosessering.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Kjører behandlingskontroll automatisk fra start.
 */
@ApplicationScoped
@ProsessTask("behandlingskontroll.startBehandling")
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class StartBehandlingTask implements ProsessTaskHandler {

    private BehandlingRepository behandlingRepository;

    StartBehandlingTask() {
        // For CDI proxy
    }

    @Inject
    public StartBehandlingTask(BehandlingRepositoryProvider repositoryProvider) {
        behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    public void doTask(ProsessTaskData data) {

        // dynamisk lookup siden finnes ikke nødvendigvis (spesielt når vi kompilerer)
        var cdi = CDI.current();
        var behandlingskontrollTjeneste = cdi.select(BehandlingskontrollTjeneste.class).get();

        try {
            var behandlingId = getBehandlingId(data);
            var lås = behandlingRepository.taSkriveLås(behandlingId);
            var behandling = behandlingRepository.hentBehandling(behandlingId);
            var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling, lås);
            // TODO (FC): assert at behandlingen starter fra første steg?
            behandlingskontrollTjeneste.prosesserBehandling(kontekst);
        } finally {
            // ikke nødvendig siden vi kjører request scoped, men tar en tidlig destroy
            cdi.destroy(behandlingskontrollTjeneste);
        }
    }

    private Long getBehandlingId(ProsessTaskData data) {
        return data.getBehandlingIdAsLong();
    }
}
