package no.nav.foreldrepenger.mottak.vedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(StartBerørtBehandlingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class StartBerørtBehandlingTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "iverksetteVedtak.startBerørtBehandling";
    private static final Logger log = LoggerFactory.getLogger(StartBerørtBehandlingTask.class);
    private BerørtBehandlingKontroller tjeneste;

    StartBerørtBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public StartBerørtBehandlingTask(BerørtBehandlingKontroller tjeneste) {
        this.tjeneste = tjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long fagsakId = prosessTaskData.getFagsakId();
        Long behandlingId = prosessTaskData.getBehandlingId();

        tjeneste.vurderNesteOppgaveIBehandlingskø(behandlingId);
        log.info("Utført for fagsak: {}", fagsakId);
    }
}
