package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import contract.sob.dto.BehandlingAvsluttet;
import contract.sob.dto.BehandlingOpprettet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.kafka.PersonoversiktHendelseProducer;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.kafka.SakOgBehandlingHendelseProducer;
import no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.task.SakOgBehandlingTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

class SakOgBehandlingKafkaTaskTest extends EntityManagerAwareTest {

    private SakOgBehandlingTask observer;
    private BehandlingRepositoryProvider repositoryProvider;

    private SakOgBehandlingHendelseProducer producer;

    @BeforeEach
    public void setup() {

        producer = mock(SakOgBehandlingHendelseProducer.class);

        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        observer = new SakOgBehandlingTask(producer, mock(PersonoversiktHendelseProducer.class), repositoryProvider);
    }

    @Test
    void skalOppretteOppdaterSakOgBehandlingTaskMedAlleParametereNårBehandlingErOpprettet() {

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));

        final var behandling = scenario.lagre(repositoryProvider);
        var fagsak = behandling.getFagsak();
        var task = ProsessTaskData.forProsessTask(SakOgBehandlingTask.class);
        task.setBehandling(fagsak.getId(), behandling.getId(), fagsak.getAktørId().getId());

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

        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        behandling = repositoryProvider.getBehandlingRepository().hentBehandling(behandling.getId());
        var fagsak =behandling.getFagsak();
        var task = ProsessTaskData.forProsessTask(SakOgBehandlingTask.class);
        task.setBehandling(fagsak.getId(), behandling.getId(), fagsak.getAktørId().getId());

        var captorKey = ArgumentCaptor.forClass(String.class);
        var captorVal = ArgumentCaptor.forClass(String.class);
        observer.doTask(task);

        verify(producer).sendJsonMedNøkkel(captorKey.capture(), captorVal.capture());
        var key = captorKey.getValue();
        var value = captorVal.getValue();
        var roundtrip = StandardJsonConfig.fromJson(value, BehandlingAvsluttet.class);
        assertThat(roundtrip.getBehandlingsID()).isEqualToIgnoringCase(Fagsystem.FPSAK.getOffisiellKode() + "_" + behandling.getId());
        assertThat(roundtrip.getAvslutningsstatus().getValue()).isEqualTo("ok");
    }


}
