package no.nav.foreldrepenger.behandlingslager.task;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Task som utfører noe på en behandling, før prosessen kjøres videre.
 * Sikrer at behandlingslås task på riktig plass.
 * Tasks som forsøker å kjøre behandling videre bør extende denne.
 */
public abstract class BehandlingProsessTask implements ProsessTaskHandler {

    private BehandlingLåsRepository behandlingLåsRepository;

    protected BehandlingProsessTask(BehandlingLåsRepository BehandlingLåsRepository) {
        this.behandlingLåsRepository = BehandlingLåsRepository;
    }

    protected BehandlingProsessTask() {
        // for CDI proxy
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var behandlingId = prosessTaskData.getBehandlingIdAsLong();
        behandlingLåsRepository.taLås(behandlingId);

        prosesser(prosessTaskData, behandlingId);
    }

    protected abstract void prosesser(ProsessTaskData prosessTaskData, Long behandlingId);

}
