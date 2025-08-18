package no.nav.foreldrepenger.behandling.kabal;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "kabal.sendtilkabal", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SendTilKabalTask extends BehandlingProsessTask {

    public static final String HJEMMEL_KEY = "hjemmel";

    private BehandlingRepository behandlingRepository;
    private KabalTjeneste kabalTjeneste;

    SendTilKabalTask() {
        // for CDI proxy
    }

    @Inject
    public SendTilKabalTask(BehandlingRepositoryProvider repositoryProvider,
                            KabalTjeneste kabalTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.kabalTjeneste = kabalTjeneste;

    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {

        var klagehjemmel = Optional.ofNullable(prosessTaskData.getPropertyValue(HJEMMEL_KEY))
            .map(KlageHjemmel::fraKode).orElse(null);
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            kabalTjeneste.sendKlageTilKabal(behandling, klagehjemmel);
        } else {
            throw new IllegalArgumentException("Utviklerfeil: Forsøker sende annen behandlingtype til KABAL");
        }
    }
}
