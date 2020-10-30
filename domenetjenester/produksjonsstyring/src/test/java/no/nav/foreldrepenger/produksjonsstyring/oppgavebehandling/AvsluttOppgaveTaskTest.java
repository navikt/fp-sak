package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTask;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OppgaveRestKlient;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ExtendWith(MockitoExtension.class)
public class AvsluttOppgaveTaskTest extends EntityManagerAwareTest {

    private OppgaveTjeneste oppgaveTjeneste;

    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;

    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private OppgaveRestKlient oppgaveRestKlient;
    @Mock
    private ProsessTaskRepository prosessTaskRepository;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        oppgaveBehandlingKoblingRepository = new OppgaveBehandlingKoblingRepository(entityManager);
        oppgaveTjeneste = new OppgaveTjeneste(new FagsakRepository(entityManager), new BehandlingRepository(entityManager),
            oppgaveBehandlingKoblingRepository, oppgaveRestKlient, prosessTaskRepository, personinfoAdapter);
    }

    @Test
    public void skal_utføre_tasken_avslutt_oppgave() {

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel();
        Behandling behandling = scenario.lagre(new BehandlingRepositoryProvider(getEntityManager()));
        Fagsak fagsak = scenario.getFagsak();

        String oppgaveId = "99";
        OppgaveBehandlingKobling oppgave = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, oppgaveId,
                fagsak.getSaksnummer(), behandling.getId());
        oppgaveBehandlingKoblingRepository.lagre(oppgave);

        ProsessTaskData taskData = new ProsessTaskData(AvsluttOppgaveTask.TASKTYPE);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        taskData.setOppgaveId(oppgaveId);
        AvsluttOppgaveTask task = new AvsluttOppgaveTask(oppgaveTjeneste);

        Mockito.doNothing().when(oppgaveRestKlient).ferdigstillOppgave(eq(oppgaveId));

        // Act
        task.doTask(taskData);

        // Assert
        var oppgaveKoblinger = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        assertThat(oppgaveKoblinger.get(0).isFerdigstilt()).isTrue();
    }

}
