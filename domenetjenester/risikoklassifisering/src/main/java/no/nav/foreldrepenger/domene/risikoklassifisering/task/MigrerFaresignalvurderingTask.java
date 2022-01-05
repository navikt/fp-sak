package no.nav.foreldrepenger.domene.risikoklassifisering.task;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
@ProsessTask("faresignalvurdering.migrer")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class MigrerFaresignalvurderingTask implements ProsessTaskHandler {
    public static final String BEHANDLING_ID = "behandlingId";

    private RisikovurderingTjeneste risikovurderingTjeneste;
    private BehandlingRepository behandlingRepository;

    MigrerFaresignalvurderingTask() {
        // for CDI proxy
    }

    @Inject
    public MigrerFaresignalvurderingTask(RisikovurderingTjeneste risikovurderingTjeneste,
                                         BehandlingRepository behandlingRepository) {
        this.risikovurderingTjeneste = risikovurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        prosesser(prosessTaskData);
    }

    private void prosesser(ProsessTaskData prosessTaskData) {
        Optional.ofNullable(prosessTaskData.getPropertyValue(BEHANDLING_ID))
            .map(Long::parseLong)
            .flatMap(behandlingRepository::finnUnikBehandlingForBehandlingId)
            .map(BehandlingReferanse::fra)
            .ifPresent(ref -> risikovurderingTjeneste.migrerFaresignalvurderingTilFprisk(ref));
    }

}
