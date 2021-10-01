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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.GjenopptaBehandlingTask;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
public class GjenopptaBehandlingTaskTest {

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
    public void skal_gjenoppta_behandling() {
        final Long behandlingId = 10L;

        var scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel();
        var behandling = scenario.lagMocked();
        when(mockBehandlingRepository.hentBehandling(any(Long.class))).thenReturn(behandling);

        var prosessTaskData = ProsessTaskData.forProsessTask(GjenopptaBehandlingTask.class);
        prosessTaskData.setBehandling(0L, behandlingId, AktørId.dummy().getId());

        task.doTask(prosessTaskData);

        verify(mockBehandlingskontrollTjeneste).initBehandlingskontroll(anyLong());
    }

}
