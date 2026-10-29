package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
class LukkForespørslerImTaskTest {
    @Mock
    private FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;

    @Test
    void historisk_utført_task_ignoreres() {
        var task = new LukkForespørslerImTask(fpInntektsmeldingTjeneste);

        task.prosesser(taskdata(ForespørselStatus.UTFØRT.name()), null, null);

        verifyNoInteractions(fpInntektsmeldingTjeneste);
    }

    @Test
    void utgått_task_setter_forespørsel_til_utgått() {
        var task = new LukkForespørslerImTask(fpInntektsmeldingTjeneste);

        task.prosesser(taskdata(ForespørselStatus.UTGÅTT.name()), null, null);

        verify(fpInntektsmeldingTjeneste).settForespørselTilUtgått("123");
    }

    @Test
    void ukjent_status_feiler_tydelig() {
        var task = new LukkForespørslerImTask(fpInntektsmeldingTjeneste);

        var exception = assertThrows(IllegalStateException.class, () -> task.prosesser(taskdata("UKJENT"), null, null));

        assertEquals("Ugyldig status: UKJENT", exception.getMessage());
        verifyNoInteractions(fpInntektsmeldingTjeneste);
    }

    @Test
    void manglende_status_feiler_tydelig() {
        var task = new LukkForespørslerImTask(fpInntektsmeldingTjeneste);

        var exception = assertThrows(IllegalStateException.class,
            () -> task.prosesser(taskdata(null), null, null));

        assertEquals("Ugyldig status: null", exception.getMessage());
        verifyNoInteractions(fpInntektsmeldingTjeneste);
    }

    private static ProsessTaskData taskdata(String status) {
        var taskdata = ProsessTaskData.forTaskType(TaskType.forProsessTask(LukkForespørslerImTask.class));
        taskdata.setProperty(LukkForespørslerImTask.SAK_NUMMER, "123");
        if (status != null) {
            taskdata.setProperty(LukkForespørslerImTask.STATUS, status);
        }
        return taskdata;
    }
}
