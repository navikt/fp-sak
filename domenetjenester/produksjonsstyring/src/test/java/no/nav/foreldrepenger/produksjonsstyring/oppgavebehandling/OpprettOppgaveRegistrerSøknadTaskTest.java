package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveRegistrerSøknadTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class OpprettOppgaveRegistrerSøknadTaskTest {

    private static final long BEHANDLING_ID = 1L;

    private OppgaveTjeneste oppgaveTjeneste;
    private OpprettOppgaveRegistrerSøknadTask opprettOppgaveRegistrerSøknadTask;

    @Before
    public void before() {
        oppgaveTjeneste = mock(OppgaveTjeneste.class);
        opprettOppgaveRegistrerSøknadTask = new OpprettOppgaveRegistrerSøknadTask(oppgaveTjeneste);
    }

    @Test
    public void skal_opprette_oppgave_for_å_registere_søknad() {
        // Arrange
        ProsessTaskData prosessTaskData = new ProsessTaskData(OpprettOppgaveRegistrerSøknadTask.TASKTYPE);
        prosessTaskData.setBehandling(1L, BEHANDLING_ID, "99");
        ArgumentCaptor<Long> behandlingIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<OppgaveÅrsak> årsakCaptor = ArgumentCaptor.forClass(OppgaveÅrsak.class);

        // Act
        opprettOppgaveRegistrerSøknadTask.doTask(prosessTaskData);

        // Assert
        verify(oppgaveTjeneste).opprettBasertPåBehandlingId(behandlingIdCaptor.capture(), årsakCaptor.capture());
        assertThat(behandlingIdCaptor.getValue()).isEqualTo(BEHANDLING_ID);
        assertThat(årsakCaptor.getValue()).isEqualTo(OppgaveÅrsak.REGISTRER_SØKNAD);
    }
}
