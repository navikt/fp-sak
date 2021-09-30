package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveForBehandlingSendtTilbakeTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
public class OpprettOppgaveForBehandlingSendtTilbakeTaskTest {
    private static final String BEHANDLENDE_ENHET_ID = "1234";

    @Mock
    private OppgaveTjeneste oppgaveTjeneste;
    private OpprettOppgaveForBehandlingSendtTilbakeTask task;
    private Behandling behandling;
    private ProsessTaskData taskData;

    @BeforeEach
    public void setup() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medBehandlendeEnhet(BEHANDLENDE_ENHET_ID);
        behandling = scenario.lagMocked();
        task = new OpprettOppgaveForBehandlingSendtTilbakeTask(oppgaveTjeneste);

        taskData = ProsessTaskData.forProsessTask(OpprettOppgaveForBehandlingSendtTilbakeTask.class);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        when(oppgaveTjeneste.opprettBehandleOppgaveForBehandlingMedPrioritetOgFrist(anyLong(), anyString(), anyBoolean(), anyInt()))
                .thenReturn("54321");
    }

    @Test
    public void shouldCallOppgaveTjeneste() {
        // Act
        task.doTask(taskData);

        // Assert
        verify(oppgaveTjeneste).opprettBehandleOppgaveForBehandlingMedPrioritetOgFrist(
                eq(behandling.getId()),
                anyString(),
                eq(true),
                eq(0));
    }
}
