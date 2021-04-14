package no.nav.foreldrepenger.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;

public class BehandleNegativeKvitteringTjenesteTest {

    private final static String TASKTYPE = "iverksetteVedtak.oppdragTilØkonomi";
    private final static Long BEHANDLING_ID = 100010010L;
    private final static Long FAGSAK_ID = 987654301L;
    private final static String AKTØR_ID = "AA-BB-CC-DD-EE";

    private ProsessTaskRepository prosessTaskRepository;

    private BehandleNegativeKvitteringTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        prosessTaskRepository = mock(ProsessTaskRepository.class);
        tjeneste = new BehandleNegativeKvitteringTjeneste(prosessTaskRepository);
    }

    @Test
    public void skal_nullstille_hendelse() {
        var taskData = lagØkonomioppragTaskPåVent();

        when(prosessTaskRepository.finn(taskData.getId())).thenReturn(taskData);

        tjeneste.nullstilleØkonomioppdragTask(taskData.getId());

        verify(prosessTaskRepository).lagre(taskData);

        assertThat(taskData.getStatus()).isEqualTo(ProsessTaskStatus.FEILET);
        assertThat(taskData.getSisteFeil()).isEqualTo("{\"feilmelding\":\"Det finnes negativ kvittering for minst en av oppdragsmottakerne.\"}");
        assertThat(taskData.getHendelse()).isEmpty();
    }

    @Test
    public void skal_kaste_IllegalStateException_hvis_task_finnes_ikke() {
        var thrown = Assertions.assertThrows(
            IllegalStateException.class,
            () -> tjeneste.nullstilleØkonomioppdragTask(0L));

        Assertions.assertTrue(thrown.getMessage().contains("Prosess task med prossess task id = 0 finnes ikke"));
    }

    private ProsessTaskData lagØkonomioppragTaskPåVent() {
        var taskData = new ProsessTaskData(TASKTYPE);
        taskData.setBehandling(FAGSAK_ID, BEHANDLING_ID, AKTØR_ID);
        taskData.venterPåHendelse(ProsessTaskHendelse.ØKONOMI_OPPDRAG_KVITTERING);
        return taskData;
    }
}
