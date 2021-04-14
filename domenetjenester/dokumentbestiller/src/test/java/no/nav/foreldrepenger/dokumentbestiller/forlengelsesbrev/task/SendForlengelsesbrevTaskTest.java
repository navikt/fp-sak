package no.nav.foreldrepenger.dokumentbestiller.forlengelsesbrev.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
public class SendForlengelsesbrevTaskTest {

    private final LocalDate nå = LocalDate.now();

    private SendForlengelsesbrevTask sendForlengelsesbrevTask;

    @Mock
    private ProsessTaskData prosessTaskData;

    @Mock
    private BehandlingRepositoryProvider behandlingRepositoryProvider;

    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    @Mock
    private BehandlingskontrollKontekst behandlingskontrollKontekst;

    private ScenarioMorSøkerEngangsstønad scenario;

    @BeforeEach
    public void setUp() {
        when(behandlingRepositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(behandlingRepositoryProvider.getBehandlingLåsRepository()).thenReturn(mock(BehandlingLåsRepository.class));
        sendForlengelsesbrevTask = new SendForlengelsesbrevTask(behandlingRepositoryProvider, behandlingskontrollTjeneste);
        when(behandlingskontrollTjeneste.initBehandlingskontroll(any(Long.class))).thenReturn(behandlingskontrollKontekst);
        scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
    }

    @Test
    public void testSkalSendeBrevOgOppdatereBehandling() {
        // Arrange
        var behandling = scenario.medBehandlingstidFrist(nå.minusDays(1))
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .lagMocked();
        when(prosessTaskData.getBehandlingId()).thenReturn(behandling.getId().toString());
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        assertThat(behandling.getBehandlingstidFrist()).isBefore(nå);

        // Act
        sendForlengelsesbrevTask.doTask(prosessTaskData);

        // Assert
        var behandlingCaptor = ArgumentCaptor.forClass(Behandling.class);
        var behandlingLåsCaptor = ArgumentCaptor.forClass(BehandlingLås.class);

        verify(behandlingRepository).lagre(behandlingCaptor.capture(), behandlingLåsCaptor.capture());

        assertThat(behandlingCaptor.getValue().getBehandlingstidFrist())
                .withFailMessage("Behandlingtype: " + behandling.getType().getKode() + " med behandlingstidsfristuker: "
                        + behandling.getType().getBehandlingstidFristUker())
                .isAfter(nå);
    }

    @Test
    public void testSkalIkkeSendeBrevOgIkkeOppdatereBehandling() {
        // Arrange
        var behandling = scenario.medBehandlingstidFrist(nå.plusDays(1)).lagMocked();
        when(prosessTaskData.getBehandlingId()).thenReturn(behandling.getId().toString());
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        assertThat(behandling.getBehandlingstidFrist()).isAfter(nå);

        // Act
        sendForlengelsesbrevTask.doTask(prosessTaskData);

        // Assert
        assertThat(behandling.getBehandlingstidFrist()).isAfter(nå);
        verify(behandlingRepository, never()).lagre(eq(behandling), any());
    }
}
