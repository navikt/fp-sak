package no.nav.foreldrepenger.økonomi.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.økonomi.økonomistøtte.BehandleNegativeKvitteringTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class BehandleNegativeKvitteringTjenesteTest {

    private final String TASKTYPE = "iverksetteVedtak.oppdragTilØkonomi";
    private final Long BEHANDLING_ID = 100010010L;
    private final Long FAGSAK_ID = 987654301L;
    private final String AKTØR_ID = "AA-BB-CC-DD-EE";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final EntityManager entityManager = repoRule.getEntityManager();
    private Repository repository = repoRule.getRepository();

    private ProsessTaskRepository prosessTaskRepository;
    private BehandleNegativeKvitteringTjeneste tjeneste;

    @Before
    public void setUp() {
        prosessTaskRepository = spy(new ProsessTaskRepositoryImpl(entityManager, null, null));
        tjeneste = new BehandleNegativeKvitteringTjeneste(prosessTaskRepository);
    }

    @Test
    public void skal_nullstille_hendelse() {
        ProsessTaskData taskData = lagreØkonomioppragTaskPåVent();

        tjeneste.nullstilleØkonomioppdragTask(taskData.getId());

        taskData = prosessTaskRepository.finn(taskData.getId());
        verify(prosessTaskRepository, times(2)).lagre(any(ProsessTaskData.class));
        assertThat(taskData.getStatus()).isEqualTo(ProsessTaskStatus.FEILET);
        assertThat(taskData.getSisteFeil()).isEqualTo("{\"feilmelding\":\"Det finnes negativ kvittering for minst en av oppdragsmottakerne.\"}");
        assertThat(taskData.getHendelse()).isEmpty();
    }

    @Test
    public void skal_kaste_IllegalStateException_hvis_task_finnes_ikke() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Prosess task med prossess task id = 0 finnes ikke");
        tjeneste.nullstilleØkonomioppdragTask(0L);
    }

    private ProsessTaskData lagreØkonomioppragTaskPåVent() {
        ProsessTaskData taskData = new ProsessTaskData(TASKTYPE);
        taskData.setBehandling(FAGSAK_ID, BEHANDLING_ID, AKTØR_ID);
        taskData.venterPåHendelse(ProsessTaskHendelse.ØKONOMI_OPPDRAG_KVITTERING);
        prosessTaskRepository.lagre(taskData);
        repository.flush();
        return taskData;
    }
}
