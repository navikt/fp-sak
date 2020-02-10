package no.nav.foreldrepenger.behandling.kriterie.impl;

import static no.nav.foreldrepenger.behandling.kriterie.BehandlingsfristUtløptTjeneste.FORLENGELSESBREV_TASK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandling.kriterie.BehandlingsfristUtløptTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

public class BehandlingsfristUtløptTjenesteImplTest {

    private static final Long FAGSAK_ID = 1L;
    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final Long BEHANDLING_ID = 3L;

    private ProsessTaskRepository prosessTaskRepository;
    private BehandlingsfristUtløptTjeneste behandlingsfristUtløptTjeneste;
    private Behandling behandling;

    @Before
    public void before() {
        prosessTaskRepository = mock(ProsessTaskRepository.class);
        behandling = mock(Behandling.class);
        when(behandling.getAktørId()).thenReturn(AKTØR_ID);
        when(behandling.getFagsakId()).thenReturn(FAGSAK_ID);
        when(behandling.getId()).thenReturn(BEHANDLING_ID);
        behandlingsfristUtløptTjeneste = new BehandlingsfristUtløptTjeneste(prosessTaskRepository);
    }

    @Test
    public void skal_opprette_prosesstask_for_behandling() {
        // Arrange
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        behandlingsfristUtløptTjeneste.behandlingsfristUtløpt(behandling);

        // Assert
        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(FORLENGELSESBREV_TASK);
        assertThat(new AktørId(prosessTaskData.getAktørId())).isEqualTo(AKTØR_ID);
        assertThat(prosessTaskData.getBehandlingId()).isEqualTo(BEHANDLING_ID);
        assertThat(prosessTaskData.getFagsakId()).isEqualTo(FAGSAK_ID);
    }
}
