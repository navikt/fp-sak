package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task;


import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("oppgavebehandling.avsluttOppgave")
public class AvsluttOppgaveTask implements ProsessTaskHandler {

    private static final String OPPGAVE_ID_TASK_KEY = "oppgaveId";

    private OppgaveTjeneste oppgaveTjeneste;

    AvsluttOppgaveTask() {
        // for CDI proxy
    }

    @Inject
    public AvsluttOppgaveTask(OppgaveTjeneste oppgaveTjeneste) {
        super();
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var oppgaveId = Optional.ofNullable(prosessTaskData.getPropertyValue(OPPGAVE_ID_TASK_KEY))
            .orElseThrow(() -> new IllegalStateException("Mangler oppgaveId"));

        oppgaveTjeneste.avslutt(oppgaveId);
    }

    public static void setOppgaveId(ProsessTaskData prosessTaskData, String oppgaveId) {
        prosessTaskData.setProperty(OPPGAVE_ID_TASK_KEY, oppgaveId);
    }
}
