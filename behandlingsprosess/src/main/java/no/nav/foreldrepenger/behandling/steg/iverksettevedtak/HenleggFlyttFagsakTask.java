package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "behandlingskontroll.henleggBehandling", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class HenleggFlyttFagsakTask extends BehandlingProsessTask {

    public static final String HENLEGGELSE_TYPE_KEY = "henleggesGrunn";

    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;

    HenleggFlyttFagsakTask() {
        // for CDI proxy
    }

    @Inject
    public HenleggFlyttFagsakTask(BehandlingRepositoryProvider repositoryProvider,
            HenleggBehandlingTjeneste henleggBehandlingTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;

    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var henleggelseType = Optional.ofNullable(prosessTaskData.getPropertyValue(HENLEGGELSE_TYPE_KEY))
            .map(BehandlingResultatType::fraKode)
            .orElseThrow();

        henleggBehandlingTjeneste.henleggBehandlingAvbrytAutopunkter(behandlingId, henleggelseType, "Forvaltning");
    }
}
