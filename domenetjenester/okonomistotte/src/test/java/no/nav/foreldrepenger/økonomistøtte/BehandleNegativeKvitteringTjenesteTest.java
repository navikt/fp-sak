package no.nav.foreldrepenger.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;

public class BehandleNegativeKvitteringTjenesteTest extends EntityManagerAwareTest {

    private final static String TASKTYPE = "iverksetteVedtak.oppdragTilØkonomi";
    private final static Long BEHANDLING_ID = 100010010L;
    private final static Long FAGSAK_ID = 987654301L;
    private final static String AKTØR_ID = "AA-BB-CC-DD-EE";

    private ProsessTaskRepository prosessTaskRepository;
    private BehandleNegativeKvitteringTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        prosessTaskRepository = spy(new ProsessTaskRepositoryImpl(getEntityManager(), null, null));
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
        ThrowableAssert.ThrowingCallable throwingCallable = () -> tjeneste.nullstilleØkonomioppdragTask(0L);
        assertThatThrownBy(throwingCallable).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(throwingCallable).hasMessageContaining("Prosess task med prossess task id = 0 finnes ikke");
    }

    private ProsessTaskData lagreØkonomioppragTaskPåVent() {
        ProsessTaskData taskData = new ProsessTaskData(TASKTYPE);
        taskData.setBehandling(FAGSAK_ID, BEHANDLING_ID, AKTØR_ID);
        taskData.venterPåHendelse(ProsessTaskHendelse.ØKONOMI_OPPDRAG_KVITTERING);
        prosessTaskRepository.lagre(taskData);
        return taskData;
    }
}
