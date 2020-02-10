package no.nav.foreldrepenger.domene.vedtak.fp;

import static no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett.VEDTAK_TIL_DATAVAREHUS_TASK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.ekstern.SettUtbetalingPåVentPrivatArbeidsgiverTask;
import no.nav.foreldrepenger.domene.vedtak.ekstern.VurderOppgaveArenaTask;
import no.nav.foreldrepenger.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SettFagsakRelasjonAvsluttningsdatoTask;
import no.nav.foreldrepenger.domene.vedtak.task.VurderOgSendØkonomiOppdragTask;
import no.nav.foreldrepenger.domene.vedtak.task.VurderOppgaveTilbakekrevingTask;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTaskProperties;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;

public class OpprettProsessTaskIverksettTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private ProsessTaskRepository prosessTaskRepository = new ProsessTaskRepositoryImpl(repoRule.getEntityManager(), null, null);

    @Mock
    private OppgaveTjeneste oppgaveTjeneste;

    private Behandling behandling;
    private OpprettProsessTaskIverksett opprettProsessTaskIverksettFP;

    @Before
    public void setup() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagMocked();
        opprettProsessTaskIverksettFP = new OpprettProsessTaskIverksettImpl(prosessTaskRepository, oppgaveTjeneste);
    }

    @Test
    public void skalIkkeAvslutteOppgave() {
        // Arrange
        when(oppgaveTjeneste.opprettTaskAvsluttOppgave(any(Behandling.class), any(OppgaveÅrsak.class), anyBoolean())).thenReturn(Optional.empty());

        // Act
        opprettProsessTaskIverksettFP.opprettIverksettingstasker(behandling, Collections.emptyList());

        // Assert
        List<ProsessTaskData> prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE,
            VurderOgSendØkonomiOppdragTask.TASKTYPE, SettUtbetalingPåVentPrivatArbeidsgiverTask.TASKTYPE,
            VurderOppgaveArenaTask.TASKTYPE, VEDTAK_TIL_DATAVAREHUS_TASK, VurderOppgaveTilbakekrevingTask.TASKTYPE,
            SettFagsakRelasjonAvsluttningsdatoTask.TASKTYPE);
    }

    @Test
    public void testOpprettIverksettingstasker() {
        // Arrange
        mockOpprettTaskAvsluttOppgave();

        // Act
        opprettProsessTaskIverksettFP.opprettIverksettingstasker(behandling, Collections.emptyList());

        // Assert
        List<ProsessTaskData> prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE,
            AvsluttOppgaveTaskProperties.TASKTYPE, VurderOgSendØkonomiOppdragTask.TASKTYPE,
            SettUtbetalingPåVentPrivatArbeidsgiverTask.TASKTYPE,
            VurderOppgaveArenaTask.TASKTYPE, VEDTAK_TIL_DATAVAREHUS_TASK, VurderOppgaveTilbakekrevingTask.TASKTYPE,
            SettFagsakRelasjonAvsluttningsdatoTask.TASKTYPE);
    }

    private void mockOpprettTaskAvsluttOppgave() {
        ProsessTaskData prosessTaskData = new ProsessTaskData(AvsluttOppgaveTaskProperties.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setOppgaveId("1001");
        when(oppgaveTjeneste.opprettTaskAvsluttOppgave(any(Behandling.class), any(OppgaveÅrsak.class), anyBoolean())).thenReturn(Optional.of(prosessTaskData));
    }
}
