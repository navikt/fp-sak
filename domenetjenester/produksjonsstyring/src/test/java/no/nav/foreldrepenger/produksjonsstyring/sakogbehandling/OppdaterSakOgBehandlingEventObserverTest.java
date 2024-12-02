package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent.BehandlingAvsluttetEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent.BehandlingOpprettetEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
class OppdaterSakOgBehandlingEventObserverTest {

    private OppdaterSakOgBehandlingEventObserver observer;
    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private ProsessTaskTjeneste taskTjenesteMock;

    @Test
    void skalOppretteOppdaterSakOgBehandlingTaskMedAlleParametereNårBehandlingErOpprettet() {

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));

        var behandling = scenario.lagMocked();
        var fagsak = behandling.getFagsak();

        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        observer = new OppdaterSakOgBehandlingEventObserver(repositoryProvider, taskTjenesteMock);

        var kontekst = new BehandlingskontrollKontekst(fagsak.getSaksnummer(), fagsak.getId(), scenario.taSkriveLåsForBehandling());
        BehandlingOpprettetEvent event = BehandlingStatusEvent.nyEvent(kontekst, BehandlingStatus.OPPRETTET);

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        observer.observerBehandlingStatus(event);

        verify(taskTjenesteMock).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        verifiserProsessTaskData(scenario, prosessTaskData, BehandlingStatus.OPPRETTET);

    }

    @Test
    void skalOppretteOppdaterSakOgBehandlingTaskMedAlleParametereNårBehandlingErAvsluttet() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));

        var behandling = scenario.lagMocked();
        var fagsak = behandling.getFagsak();

        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        observer = new OppdaterSakOgBehandlingEventObserver(repositoryProvider, taskTjenesteMock);

        var kontekst = new BehandlingskontrollKontekst(fagsak.getSaksnummer(), fagsak.getId(), scenario.taSkriveLåsForBehandling());
        BehandlingAvsluttetEvent event = BehandlingStatusEvent.nyEvent(kontekst, BehandlingStatus.AVSLUTTET);

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        observer.observerBehandlingStatus(event);

        verify(taskTjenesteMock).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        verifiserProsessTaskData(scenario, prosessTaskData, BehandlingStatus.AVSLUTTET);
    }

    private void verifiserProsessTaskData(ScenarioMorSøkerEngangsstønad scenario, ProsessTaskData prosessTaskData, BehandlingStatus expected) {
        assertThat(prosessTaskData.taskType()).isEqualTo(TaskType.forProsessTask(OppdaterPersonoversiktTask.class));
        assertThat(prosessTaskData.getFagsakId()).isEqualTo(scenario.getFagsak().getId());
        assertThat(prosessTaskData.getBehandlingId()).isEqualTo(scenario.getBehandling().getId().toString());
        assertThat(prosessTaskData.getPropertyValue(OppdaterPersonoversiktTask.PH_REF_KEY)).contains(scenario.getBehandling().getId().toString());
        assertThat(prosessTaskData.getPropertyValue(OppdaterPersonoversiktTask.PH_STATUS_KEY)).isEqualTo(expected.getKode());
        assertThat(LocalDateTime.parse(prosessTaskData.getPropertyValue(OppdaterPersonoversiktTask.PH_TID_KEY), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .isBefore(LocalDateTime.now().plusNanos(1000));
        assertThat(prosessTaskData.getPropertyValue(OppdaterPersonoversiktTask.PH_TYPE_KEY)).isEqualTo(BehandlingType.FØRSTEGANGSSØKNAD.getKode());
    }

}
