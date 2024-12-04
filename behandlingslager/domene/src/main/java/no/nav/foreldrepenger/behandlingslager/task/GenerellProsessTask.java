package no.nav.foreldrepenger.behandlingslager.task;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Generell task som ikke låser behandling eller sak før kall til prosesser
 */
public abstract class GenerellProsessTask implements ProsessTaskHandler {

    protected GenerellProsessTask() {
        // for CDI proxy
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fagsakId = prosessTaskData.getFagsakId();
        var behandlingId = getBehandlingId(prosessTaskData);

        prosesser(prosessTaskData, fagsakId, behandlingId);
    }

    protected abstract void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId);

    private Long getBehandlingId(ProsessTaskData data) {
        return data.getBehandlingIdAsLong();
    }
}
