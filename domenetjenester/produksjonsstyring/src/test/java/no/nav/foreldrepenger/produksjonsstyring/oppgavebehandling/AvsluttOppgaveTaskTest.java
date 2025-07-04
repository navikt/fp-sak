package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTask;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgaver;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(MockitoExtension.class)
public class AvsluttOppgaveTaskTest  {

    private OppgaveTjeneste oppgaveTjeneste;

    @Mock
    private Oppgaver oppgaveRestKlient;
    @Mock
    private ProsessTaskTjeneste taskTjeneste;

    @BeforeEach
    void setup() {
        oppgaveTjeneste = new OppgaveTjeneste(null, null, oppgaveRestKlient, taskTjeneste, null);
    }

    @Test
    void skal_utføre_tasken_avslutt_oppgave() {

        var oppgaveId = "99";

        var taskData = ProsessTaskData.forProsessTask(AvsluttOppgaveTask.class);
        AvsluttOppgaveTask.setOppgaveId(taskData, oppgaveId);
        var task = new AvsluttOppgaveTask(oppgaveTjeneste);

        Mockito.doNothing().when(oppgaveRestKlient).ferdigstillOppgave(eq(oppgaveId));

        // Act
        task.doTask(taskData);

        // Assert
        verify(oppgaveRestKlient).ferdigstillOppgave(oppgaveId);
    }

}
