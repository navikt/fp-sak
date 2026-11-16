package no.nav.foreldrepenger.behandlingslager.task;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
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
        var saksnummer = Saksnummer.fra(prosessTaskData.getSaksnummer());
        var behandlingId = getBehandlingId(prosessTaskData);

        prosesser(prosessTaskData, saksnummer, behandlingId);
    }

    protected abstract void prosesser(ProsessTaskData prosessTaskData, Saksnummer saksnummer, Long behandlingId);

    private Long getBehandlingId(ProsessTaskData data) {
        return data.getBehandlingIdAsLong();
    }
}
