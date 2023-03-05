package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus.Status;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
class BehandlingsprosessTjenesteTest {

    private static final String GRUPPE_1 = "gruppe1";

    private final ProsessTaskData taskData = ProsessTaskData.forTaskType(new TaskType("taskType1"));
    private Behandling behandling;

    public BehandlingsprosessTjenesteTest() {
        this.taskData.setGruppe(GRUPPE_1);
        this.behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
    }

    @Test
    void skal_returnere_gruppe_når_ikke_er_kjørt() {

        var sut = initSut(GRUPPE_1, taskData);
        var status = sut.sjekkProsessTaskPågårForBehandling(behandling, null);
        assertThat(status.get().getStatus()).isEqualTo(Status.PENDING);

        status = sut.sjekkProsessTaskPågårForBehandling(behandling, GRUPPE_1);
        assertThat(status.get().getStatus()).isEqualTo(Status.PENDING);
    }

    @Test
    void skal_ikke_returnere_gruppe_når_er_kjørt() {
        markerFerdig(taskData);

        var sut = initSut(GRUPPE_1, taskData);
        var status = sut.sjekkProsessTaskPågårForBehandling(behandling, null);
        assertThat(status).isEmpty();

        status = sut.sjekkProsessTaskPågårForBehandling(behandling, GRUPPE_1);
        assertThat(status).isEmpty();

    }

    @Test
    void skal_kaste_exception_når_task_har_feilet_null_gruppe() {
        markerFeilet(taskData);

        var sut = initSut(GRUPPE_1, taskData);
        var status = sut.sjekkProsessTaskPågårForBehandling(behandling, null);

        assertThat(status.get().getStatus()).isEqualTo(Status.HALTED);
    }

    @Test
    void skal_kaste_exception_når_task_har_feilet_angitt_gruppe() {
        markerFeilet(taskData);

        var sut = initSut(GRUPPE_1, taskData);

        var status = sut.sjekkProsessTaskPågårForBehandling(behandling, GRUPPE_1);

        assertThat(status.get().getStatus()).isEqualTo(Status.HALTED);
    }

    @Test
    void skal_kaste_exception_når_task_neste_kjøring_er_utsatt() {
        taskData.medNesteKjøringEtter(LocalDateTime.now().plusHours(1));

        var sut = initSut(GRUPPE_1, taskData);
        var status = sut.sjekkProsessTaskPågårForBehandling(behandling, GRUPPE_1);

        assertThat(status.get().getStatus()).isEqualTo(Status.DELAYED);

    }

    private static void markerFeilet(ProsessTaskData pt) {
        pt.setStatus(ProsessTaskStatus.FEILET);
        pt.setAntallFeiledeForsøk(pt.getAntallFeiledeForsøk() + 1);
        pt.setNesteKjøringEtter(null);
        pt.setSistKjørt(LocalDateTime.now());
    }

    private static void markerFerdig(ProsessTaskData pt) {
        pt.setStatus(ProsessTaskStatus.FERDIG);
        pt.setNesteKjøringEtter(null);
        pt.setSistKjørt(LocalDateTime.now());
    }

    private static BehandlingsprosessTjeneste initSut(String gruppe, ProsessTaskData taskData) {
        var tjeneste = Mockito.mock(ProsesseringAsynkTjeneste.class);

        Map<String, ProsessTaskData> data = new HashMap<>();
        data.put(gruppe, taskData);

        when(tjeneste.sjekkProsessTaskPågårForBehandling(Mockito.any(), Mockito.any())).thenReturn(data);
        var sut = new BehandlingsprosessTjeneste(tjeneste);
        return sut;
    }
}
