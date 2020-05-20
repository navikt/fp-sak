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

import contract.sob.dto.BehandlingAvsluttet;
import contract.sob.dto.BehandlingOpprettet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.kafka.JsonObjectMapper;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.kafka.SakOgBehandlingHendelseProducer;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.task.SakOgBehandlingTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.testutilities.Whitebox;

public class SakOgBehandlingKafkaTaskTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private SakOgBehandlingTask observer;
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private SakOgBehandlingHendelseProducer producer;

    @Before
    public void setup() {

        producer = mock(SakOgBehandlingHendelseProducer.class);

        observer = new SakOgBehandlingTask(producer, repositoryProvider);
    }

    @Test
    public void skalOppretteOppdaterSakOgBehandlingTaskMedAlleParametereNårBehandlingErOpprettet() {

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));

        final Behandling behandling = scenario.lagre(repositoryProvider);
        Fagsak fagsak = behandling.getFagsak();
        refreshBehandlingType(scenario);
        var task = new ProsessTaskData(SakOgBehandlingTask.TASKTYPE);
        task.setBehandling(fagsak.getId(), behandling.getId(), fagsak.getAktørId().getId());

        ArgumentCaptor<String> captorKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> captorVal = ArgumentCaptor.forClass(String.class);
        observer.doTask(task);

        verify(producer).sendJsonMedNøkkel(captorKey.capture(), captorVal.capture());
        String key = captorKey.getValue();
        String value = captorVal.getValue();
        BehandlingOpprettet roundtrip = JsonObjectMapper.fromJson(value, BehandlingOpprettet.class);
        assertThat(roundtrip.getBehandlingsID()).isEqualToIgnoringCase(Fagsystem.FPSAK.getOffisiellKode() + "_" + behandling.getId());
    }

    @Test
    public void skalOppretteOppdaterSakOgBehandlingTaskMedAlleParametereNårBehandlingErAvsluttet() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));

        Behandling behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        behandling = repositoryProvider.getBehandlingRepository().hentBehandling(behandling.getId());
        refreshBehandlingType(scenario);
        Fagsak fagsak =behandling.getFagsak();
        var task = new ProsessTaskData(SakOgBehandlingTask.TASKTYPE);
        task.setBehandling(fagsak.getId(), behandling.getId(), fagsak.getAktørId().getId());

        ArgumentCaptor<String> captorKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> captorVal = ArgumentCaptor.forClass(String.class);
        observer.doTask(task);

        verify(producer).sendJsonMedNøkkel(captorKey.capture(), captorVal.capture());
        String key = captorKey.getValue();
        String value = captorVal.getValue();
        BehandlingAvsluttet roundtrip = JsonObjectMapper.fromJson(value, BehandlingAvsluttet.class);
        assertThat(roundtrip.getBehandlingsID()).isEqualToIgnoringCase(Fagsystem.FPSAK.getOffisiellKode() + "_" + behandling.getId());
        assertThat(roundtrip.getAvslutningsstatus().getValue()).isEqualTo("ok");
    }

    private void refreshBehandlingType(ScenarioMorSøkerEngangsstønad scenario) {
        BehandlingType behandlingType = scenario.getBehandling().getType();
        Whitebox.setInternalState(scenario.getBehandling(), "behandlingType", behandlingType);
    }


}
