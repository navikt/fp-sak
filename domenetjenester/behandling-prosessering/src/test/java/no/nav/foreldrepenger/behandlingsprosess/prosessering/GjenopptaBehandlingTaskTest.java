package no.nav.foreldrepenger.behandlingsprosess.prosessering;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.GjenopptaBehandlingTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
class GjenopptaBehandlingTaskTest {

    private GjenopptaBehandlingTask task; // objektet vi tester

    @Mock
    private BehandlingRepository mockBehandlingRepository;
    @Mock
    private BehandlingskontrollTjeneste mockBehandlingskontrollTjeneste;

    @BeforeEach
    public void setup() {
        task = new GjenopptaBehandlingTask(mockBehandlingRepository, mock(BehandlingLåsRepository.class), mockBehandlingskontrollTjeneste);
    }

    @Test
    void skal_gjenoppta_behandling() {
        final Long behandlingId = 10L;

        var scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel();
        var behandling = scenario.lagMocked();
        when(mockBehandlingRepository.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockBehandlingRepository.taSkriveLås(anyLong())).thenReturn(new BehandlingLås(behandlingId));

        var prosessTaskData = ProsessTaskData.forProsessTask(GjenopptaBehandlingTask.class);
        prosessTaskData.setBehandling("123", 0L, behandlingId);

        task.doTask(prosessTaskData);

        verify(mockBehandlingskontrollTjeneste).initBehandlingskontroll(any(Behandling.class), any(BehandlingLås.class));
    }

}
