package no.nav.foreldrepenger.domene.vedtak.es;

import static no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett.VEDTAK_TIL_DATAVAREHUS_TASK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.foreldrepenger.domene.vedtak.task.VurderOgSendØkonomiOppdragTask;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;

public class OpprettProsessTaskIverksettTest extends EntityManagerAwareTest {

    private ProsessTaskRepository prosessTaskRepository;

    private OppgaveTjeneste oppgaveTjeneste;

    private OpprettProsessTaskIverksett opprettProsessTaskIverksett;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        prosessTaskRepository = new ProsessTaskRepositoryImpl(entityManager, null, null);
        var behandlingRepository = new BehandlingRepository(entityManager);
        oppgaveTjeneste = mock(OppgaveTjeneste.class);
        opprettProsessTaskIverksett = new OpprettProsessTaskIverksett(prosessTaskRepository, behandlingRepository,
            new AnkeRepository(entityManager), new KlageRepository(entityManager), oppgaveTjeneste);
    }

    @Test
    public void skalIkkeAvslutteOppgave() {
        // Arrange
        when(oppgaveTjeneste.opprettTaskAvsluttOppgave(any(Behandling.class), any(OppgaveÅrsak.class), anyBoolean())).thenReturn(Optional.empty());

        // Act
        opprettProsessTaskIverksett.opprettIverksettingTasks(opprettBehandling());

        // Assert
        List<ProsessTaskData> prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE,
            VurderOgSendØkonomiOppdragTask.TASKTYPE, VEDTAK_TIL_DATAVAREHUS_TASK);
    }

    @Test
    public void testOpprettIverksettingstasker() {
        // Arrange
        var behandling = opprettBehandling();
        mockOpprettTaskAvsluttOppgave(behandling);

        // Act
        opprettProsessTaskIverksett.opprettIverksettingTasks(behandling);

        // Assert
        List<ProsessTaskData> prosessTaskDataList = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE,
            AvsluttOppgaveTask.TASKTYPE, VurderOgSendØkonomiOppdragTask.TASKTYPE,
            VEDTAK_TIL_DATAVAREHUS_TASK);
    }

    private Behandling opprettBehandling() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        return scenario.lagMocked();
    }

    private void mockOpprettTaskAvsluttOppgave(Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(AvsluttOppgaveTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setOppgaveId("1001");
        when(oppgaveTjeneste.opprettTaskAvsluttOppgave(any(Behandling.class), any(OppgaveÅrsak.class), anyBoolean())).thenReturn(Optional.of(prosessTaskData));
    }
}
