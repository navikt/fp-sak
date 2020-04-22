package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.rest.OppgaveRestKlient;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTaskProperties;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSFerdigstillOppgaveResponse;
import no.nav.vedtak.felles.integrasjon.behandleoppgave.BehandleoppgaveConsumer;
import no.nav.vedtak.felles.integrasjon.behandleoppgave.FerdigstillOppgaveRequestMal;
import no.nav.vedtak.felles.integrasjon.oppgave.OppgaveConsumer;
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
    private BehandleoppgaveConsumer mockService;

    @Mock
    private OppgaveConsumer oppgaveConsumer;

    @Mock
    private TpsTjeneste tpsTjeneste;

    @Mock
    private ProsessTaskRepository prosessTaskRepository;

    @Before
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        oppgaveBehandlingKoblingRepository = new OppgaveBehandlingKoblingRepository(entityManager);
        mockService = Mockito.mock(BehandleoppgaveConsumer.class);
        oppgaveConsumer = Mockito.mock(OppgaveConsumer.class);
        var oppgaveRestKlient = Mockito.mock(OppgaveRestKlient.class);
        oppgaveTjeneste = new OppgaveTjeneste(repositoryProvider, oppgaveBehandlingKoblingRepository, mockService,
            oppgaveConsumer, oppgaveRestKlient, prosessTaskRepository, tpsTjeneste);
    }

    @Test
    public void skal_utføre_tasken_avslutt_oppgave() {
        // Arrange

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);
        Fagsak fagsak = scenario.getFagsak();

        String oppgaveId = "GSAK1110";
        OppgaveBehandlingKobling oppgave = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, oppgaveId,
            fagsak.getSaksnummer(), behandling);
        oppgaveBehandlingKoblingRepository.lagre(oppgave);

        ProsessTaskData taskData = new ProsessTaskData(AvsluttOppgaveTaskProperties.TASKTYPE);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        taskData.setOppgaveId(oppgaveId);
        AvsluttOppgaveTask task = new AvsluttOppgaveTask(oppgaveTjeneste, repositoryProvider);

        WSFerdigstillOppgaveResponse mockResponse = new WSFerdigstillOppgaveResponse();
        ArgumentCaptor<FerdigstillOppgaveRequestMal> captor = ArgumentCaptor.forClass(FerdigstillOppgaveRequestMal.class);
        when(mockService.ferdigstillOppgave(captor.capture())).thenReturn(mockResponse);

        // Act
        task.doTask(taskData);

        // Assert
        FerdigstillOppgaveRequestMal result = captor.getValue();
        assertThat(result.getOppgaveId()).isEqualTo(oppgaveId);
        assertThat(result.getFerdigstiltAvEnhetId()).isNotNull();
        List<OppgaveBehandlingKobling> oppgaveKoblinger = repository.hentAlle(OppgaveBehandlingKobling.class);
        assertThat(oppgaveKoblinger.get(0).isFerdigstilt()).isTrue();
    }

}
