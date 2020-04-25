package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTaskProperties;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OppgaveRestKlient;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class AvsluttOppgaveTaskTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private Repository repository = repoRule.getRepository();
    private final EntityManager entityManager = repoRule.getEntityManager();

    private OppgaveTjeneste oppgaveTjeneste;

    private BehandlingRepositoryProvider repositoryProvider;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;

    @Mock
    private TpsTjeneste tpsTjeneste;
    @Mock
    private OppgaveRestKlient oppgaveRestKlient;
    @Mock
    private ProsessTaskRepository prosessTaskRepository;

    @Before
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        oppgaveBehandlingKoblingRepository = new OppgaveBehandlingKoblingRepository(entityManager);
        oppgaveRestKlient = Mockito.mock(OppgaveRestKlient.class);
        oppgaveTjeneste = new OppgaveTjeneste(repositoryProvider, oppgaveBehandlingKoblingRepository, oppgaveRestKlient, prosessTaskRepository, tpsTjeneste);
    }

    @Test
    public void skal_utføre_tasken_avslutt_oppgave() {
        // Arrange

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);
        Fagsak fagsak = scenario.getFagsak();

        String oppgaveId = "99";
        OppgaveBehandlingKobling oppgave = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, oppgaveId,
            fagsak.getSaksnummer(), behandling);
        oppgaveBehandlingKoblingRepository.lagre(oppgave);

        ProsessTaskData taskData = new ProsessTaskData(AvsluttOppgaveTaskProperties.TASKTYPE);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        taskData.setOppgaveId(oppgaveId);
        AvsluttOppgaveTask task = new AvsluttOppgaveTask(oppgaveTjeneste, repositoryProvider);

        Mockito.doNothing().when(oppgaveRestKlient).ferdigstillOppgave(eq(oppgaveId));

        // Act
        task.doTask(taskData);

        // Assert
        List<OppgaveBehandlingKobling> oppgaveKoblinger = repository.hentAlle(OppgaveBehandlingKobling.class);
        assertThat(oppgaveKoblinger.get(0).isFerdigstilt()).isTrue();
    }

}
