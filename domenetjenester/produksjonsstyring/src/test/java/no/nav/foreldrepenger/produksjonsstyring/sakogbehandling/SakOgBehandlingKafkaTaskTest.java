package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import contract.sob.dto.BehandlingAvsluttet;
import contract.sob.dto.BehandlingOpprettet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.kafka.PersonoversiktHendelseProducer;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.task.SakOgBehandlingTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
class SakOgBehandlingKafkaTaskTest {

    private SakOgBehandlingTask observer;
    private BehandlingRepositoryProvider repositoryProvider;
    @Mock
    private PersonoversiktHendelseProducer producer;


    @Test
    void skalOppretteOppdaterSakOgBehandlingTaskMedAlleParametereNårBehandlingErOpprettet() {

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));
        scenario.medBehandlendeEnhet("4867");
        var behandling = scenario.lagMocked();
        var fagsak = behandling.getFagsak();
        var task = ProsessTaskData.forProsessTask(SakOgBehandlingTask.class);
        task.setBehandling(fagsak.getId(), behandling.getId(), fagsak.getAktørId().getId());

        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        observer = new SakOgBehandlingTask(producer,mock(PersoninfoAdapter.class), repositoryProvider);

        var captorKey = ArgumentCaptor.forClass(String.class);
        var captorVal = ArgumentCaptor.forClass(String.class);
        observer.doTask(task);

        verify(producer).sendJsonMedNøkkel(captorKey.capture(), captorVal.capture());
        var value = captorVal.getValue();
        var roundtrip = StandardJsonConfig.fromJson(value, BehandlingOpprettet.class);
        assertThat(roundtrip.getBehandlingsID()).isEqualToIgnoringCase(Fagsystem.FPSAK.getOffisiellKode() + "_" + behandling.getId());
    }

    @Test
    void skalOppretteOppdaterSakOgBehandlingTaskMedAlleParametereNårBehandlingErAvsluttet() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));
        scenario.medBehandlendeEnhet("4867");
        var behandling = scenario.lagMocked();
        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        behandling = repositoryProvider.getBehandlingRepository().hentBehandling(behandling.getId());
        var fagsak =behandling.getFagsak();
        var task = ProsessTaskData.forProsessTask(SakOgBehandlingTask.class);
        task.setBehandling(fagsak.getId(), behandling.getId(), fagsak.getAktørId().getId());

        observer = new SakOgBehandlingTask(producer, mock(PersoninfoAdapter.class), repositoryProvider);

        var captorKey = ArgumentCaptor.forClass(String.class);
        var captorVal = ArgumentCaptor.forClass(String.class);
        observer.doTask(task);

        verify(producer).sendJsonMedNøkkel(captorKey.capture(), captorVal.capture());
        var value = captorVal.getValue();
        var roundtrip = StandardJsonConfig.fromJson(value, BehandlingAvsluttet.class);
        assertThat(roundtrip.getBehandlingsID()).isEqualToIgnoringCase(Fagsystem.FPSAK.getOffisiellKode() + "_" + behandling.getId());
        assertThat(roundtrip.getAvslutningsstatus().getValue()).isEqualTo("ok");
    }


}
