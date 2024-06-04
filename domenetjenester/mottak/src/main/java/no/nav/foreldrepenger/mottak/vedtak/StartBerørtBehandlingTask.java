package no.nav.foreldrepenger.mottak.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.mottak.sakskompleks.BerørtBehandlingKontroller;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.startBerørtBehandling", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class StartBerørtBehandlingTask extends GenerellProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(StartBerørtBehandlingTask.class);
    private BerørtBehandlingKontroller tjeneste;

    StartBerørtBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public StartBerørtBehandlingTask(BerørtBehandlingKontroller tjeneste) {
        super();
        this.tjeneste = tjeneste;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {

        tjeneste.vurderNesteOppgaveIBehandlingskø(behandlingId);
        LOG.info("Utført for fagsak: {}", fagsakId);
    }
}
