package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(OpprettOppgaveSendTilInfotrygdTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveSendTilInfotrygdTask extends GenerellProsessTask {
    public static final String TASKTYPE = "oppgavebehandling.opprettOppgaveSakTilInfotrygd";
    private static final Logger LOG = LoggerFactory.getLogger(OpprettOppgaveSendTilInfotrygdTask.class);

    private OppgaveTjeneste oppgaveTjeneste;

    OpprettOppgaveSendTilInfotrygdTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettOppgaveSendTilInfotrygdTask(OppgaveTjeneste oppgaveTjeneste) {
        super();
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        String oppgaveId = oppgaveTjeneste.opprettOppgaveSakSkalTilInfotrygd(behandlingId);
        LOG.info("Oppgave opprettet i GSAK slik at Infotrygd kan behandle saken videre. Oppgavenummer: {}", oppgaveId);
    }
}
