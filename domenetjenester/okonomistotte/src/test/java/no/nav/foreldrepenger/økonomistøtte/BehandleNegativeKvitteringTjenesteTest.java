package no.nav.foreldrepenger.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

class BehandleNegativeKvitteringTjenesteTest {

    private final static TaskType TASKTYPE = new TaskType("iverksetteVedtak.oppdragTilØkonomi"); //TODO deps
    private final static Long BEHANDLING_ID = 100010010L;
    private final static Long FAGSAK_ID = 987654301L;
    private static final Saksnummer SAKSNUMMER = new Saksnummer(FAGSAK_ID.toString());

    private ProsessTaskTjeneste taskTjeneste;

    private BehandleNegativeKvitteringTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        taskTjeneste = mock(ProsessTaskTjeneste.class);
        tjeneste = new BehandleNegativeKvitteringTjeneste(taskTjeneste);
    }

    @Test
    void skal_nullstille_hendelse() {
        var taskData = lagØkonomioppragTaskPåVent();

        when(taskTjeneste.finn(taskData.getId())).thenReturn(taskData);

        tjeneste.nullstilleØkonomioppdragTask(taskData.getId());

        verify(taskTjeneste).lagre(taskData);

        assertThat(taskData.getStatus()).isEqualTo(ProsessTaskStatus.FEILET);
        assertThat(taskData.getSisteFeil()).contains("\"Det finnes negativ kvittering for minst en av oppdragsmottakerne.\"");
        assertThat(taskData.getVentetHendelse()).isEmpty();
    }

    @Test
    void skal_kaste_IllegalStateException_hvis_task_finnes_ikke() {
        var thrown = Assertions.assertThrows(
            IllegalStateException.class,
            () -> tjeneste.nullstilleØkonomioppdragTask(0L));

        assertThat(thrown.getMessage()).contains("Prosess task med prossess task id = 0 finnes ikke");
    }

    private ProsessTaskData lagØkonomioppragTaskPåVent() {
        var taskData = ProsessTaskData.forTaskType(TASKTYPE);
        taskData.setBehandling(SAKSNUMMER.getVerdi(), FAGSAK_ID, BEHANDLING_ID);
        taskData.venterPåHendelse(BehandleØkonomioppdragKvittering.ØKONOMI_OPPDRAG_KVITTERING);
        return taskData;
    }
}
