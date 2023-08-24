package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent.BehandlingAvsluttetEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent.BehandlingOpprettetEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.observer.OppdaterSakOgBehandlingEventObserver;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.task.SakOgBehandlingTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

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

        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
                scenario.taSkriveLåsForBehandling());
        BehandlingOpprettetEvent event = BehandlingStatusEvent.nyEvent(kontekst, BehandlingStatus.OPPRETTET);

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        observer.observerBehandlingStatus(event);

        verify(taskTjenesteMock).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        verifiserProsessTaskData(scenario, prosessTaskData);

    }

    @Test
    void skalOppretteOppdaterSakOgBehandlingTaskMedAlleParametereNårBehandlingErAvsluttet() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));

        var behandling = scenario.lagMocked();
        var fagsak = behandling.getFagsak();

        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        observer = new OppdaterSakOgBehandlingEventObserver(repositoryProvider, taskTjenesteMock);

        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
                scenario.taSkriveLåsForBehandling());
        BehandlingAvsluttetEvent event = BehandlingStatusEvent.nyEvent(kontekst, BehandlingStatus.AVSLUTTET);

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        observer.observerBehandlingStatus(event);

        verify(taskTjenesteMock).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        verifiserProsessTaskData(scenario, prosessTaskData);
    }

    private void verifiserProsessTaskData(ScenarioMorSøkerEngangsstønad scenario, ProsessTaskData prosessTaskData) {
        assertThat(prosessTaskData.taskType()).isEqualTo(TaskType.forProsessTask(SakOgBehandlingTask.class));
        assertThat(new AktørId(prosessTaskData.getAktørId()))
                .isEqualTo(scenario.getFagsak().getNavBruker().getAktørId());
        assertThat(prosessTaskData.getBehandlingId()).isEqualTo(scenario.getBehandling().getId().toString());
    }

}
