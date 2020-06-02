package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent.BehandlingAvsluttetEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent.BehandlingOpprettetEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.observer.OppdaterSakOgBehandlingEventObserver;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.task.SakOgBehandlingTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.Whitebox;

@SuppressWarnings("deprecation")
public class OppdaterSakOgBehandlingEventObserverTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private OppdaterSakOgBehandlingEventObserver observer;
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private ProsessTaskRepository prosessTaskRepositoryMock;

    @Before
    public void setup() {

        prosessTaskRepositoryMock = mock(ProsessTaskRepository.class);

        observer = new OppdaterSakOgBehandlingEventObserver(repositoryProvider, prosessTaskRepositoryMock);
    }

    @Test
    public void skalOppretteOppdaterSakOgBehandlingTaskMedAlleParametereNårBehandlingErOpprettet() {

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));

        final Behandling behandling = scenario.lagre(repositoryProvider);
        Fagsak fagsak = behandling.getFagsak();
        refreshBehandlingType(scenario);

        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(),fagsak.getAktørId(), scenario.taSkriveLåsForBehandling());
        BehandlingOpprettetEvent event = BehandlingStatusEvent.nyEvent(kontekst, BehandlingStatus.OPPRETTET);

        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        observer.observerBehandlingStatus(event);

        verify(prosessTaskRepositoryMock).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        verifiserProsessTaskData(scenario, prosessTaskData, BehandlingStatus.OPPRETTET.getKode());

    }

    @Test
    public void skalOppretteOppdaterSakOgBehandlingTaskMedAlleParametereNårBehandlingErAvsluttet() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));

        Behandling behandling = scenario.lagre(repositoryProvider);
        refreshBehandlingType(scenario);
        Fagsak fagsak =behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(),fagsak.getAktørId(), scenario.taSkriveLåsForBehandling());
        BehandlingAvsluttetEvent event = BehandlingStatusEvent.nyEvent(kontekst, BehandlingStatus.AVSLUTTET);

        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        observer.observerBehandlingStatus(event);

        verify(prosessTaskRepositoryMock).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        verifiserProsessTaskData(scenario, prosessTaskData, BehandlingStatus.AVSLUTTET.getKode());
    }

    private void refreshBehandlingType(ScenarioMorSøkerEngangsstønad scenario) {
        BehandlingType behandlingType = scenario.getBehandling().getType();
        Whitebox.setInternalState(scenario.getBehandling(), "behandlingType", behandlingType);
    }

    private void verifiserProsessTaskData(ScenarioMorSøkerEngangsstønad scenario, ProsessTaskData prosessTaskData,
                                          String behandlingStatusKode) {
        assertThat(prosessTaskData.getTaskType()).isEqualTo(SakOgBehandlingTask.TASKTYPE);
        assertThat(new AktørId(prosessTaskData.getAktørId()))
            .isEqualTo(scenario.getFagsak().getNavBruker().getAktørId());
        assertThat(prosessTaskData.getBehandlingId())
            .isEqualTo(scenario.getBehandling().getId());
    }

}
