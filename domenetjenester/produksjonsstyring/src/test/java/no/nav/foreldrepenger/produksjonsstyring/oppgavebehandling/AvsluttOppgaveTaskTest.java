package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTask;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OppgaveRestKlient;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
@ExtendWith(MockitoExtension.class)
public class AvsluttOppgaveTaskTest extends EntityManagerAwareTest {

    private OppgaveTjeneste oppgaveTjeneste;

    private BehandlingRepositoryProvider repositoryProvider;
    private Repository repository;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;

    @Mock
    private TpsTjeneste tpsTjeneste;
    @Mock
    private OppgaveRestKlient oppgaveRestKlient;
    @Mock
    private ProsessTaskRepository prosessTaskRepository;

    @BeforeEach
    public void setup() {
        repository = new Repository(getEntityManager());
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        oppgaveBehandlingKoblingRepository = new OppgaveBehandlingKoblingRepository(getEntityManager());
        oppgaveTjeneste = new OppgaveTjeneste(repositoryProvider, oppgaveBehandlingKoblingRepository, oppgaveRestKlient, prosessTaskRepository,
                tpsTjeneste);
    }

    @Test
    public void skal_utføre_tasken_avslutt_oppgave() {

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);
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
        List<OppgaveBehandlingKobling> oppgaveKoblinger = repository.hentAlle(OppgaveBehandlingKobling.class);
        assertThat(oppgaveKoblinger.get(0).isFerdigstilt()).isTrue();
    }

}
