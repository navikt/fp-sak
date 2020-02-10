package no.nav.foreldrepenger.domene.vedtak.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.finn.unleash.FakeUnleash;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

public class VurderOgSendØkonomiOppdragTaskTest {

    private static final Long BEHANDLING_ID = 139L;

    private static final Long TASK_ID = 238L;

    private static final String AKTØR_ID = AktørId.dummy().getId();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ProsessTaskRepository repo;

    @Mock
    private ProsessTaskData prosessTaskData;

    @Mock
    private OppdragskontrollTjeneste oppdragskontrollTjeneste;

    private VurderOgSendØkonomiOppdragTask task;

    private FakeUnleash fakeUnleash = new FakeUnleash();

    @Before
    public void setUp() {
        when(prosessTaskData.getBehandlingId()).thenReturn(BEHANDLING_ID);
        when(prosessTaskData.getId()).thenReturn(TASK_ID);
        when(prosessTaskData.getAktørId()).thenReturn(AKTØR_ID);
        task = new VurderOgSendØkonomiOppdragTask(oppdragskontrollTjeneste,
            repo, ScenarioMorSøkerForeldrepenger.forFødsel().mockBehandlingRepositoryProvider(), fakeUnleash);
    }

    @Test
    public void testSkalSendeOppdrag() {
        // Arrange
        Oppdragskontroll oppdragskontroll = Oppdragskontroll.builder()
            .medBehandlingId(BEHANDLING_ID)
            .medProsessTaskId(BEHANDLING_ID)
            .medVenterKvittering(true)
            .medSaksnummer(new Saksnummer(BEHANDLING_ID.toString()))
            .build();
        when(oppdragskontrollTjeneste.opprettOppdrag(anyLong(), anyLong())).thenReturn(Optional.ofNullable(oppdragskontroll));
        when(prosessTaskData.getHendelse()).thenReturn(Optional.empty());

        // Act
        task.doTask(prosessTaskData);

        // Assert
        verify(prosessTaskData).venterPåHendelse(ProsessTaskHendelse.ØKONOMI_OPPDRAG_KVITTERING);
        verify(repo).lagre(prosessTaskData);
    }

    @Test
    public void testSkalIkkeSendeOppdrag() {
        // Arrange
        when(oppdragskontrollTjeneste.opprettOppdrag(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(prosessTaskData.getHendelse()).thenReturn(Optional.empty());

        // Act
        task.doTask(prosessTaskData);

        // Assert oppretter bare prosesstask for å sende tilkjent ytelse
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(repo).lagre(captor.capture());
        Assertions.assertThat(captor.getValue().getTaskType()).isEqualTo(SendTilkjentYtelseTask.TASKTYPE);
        verifyNoMoreInteractions(repo);

        verify(prosessTaskData, never()).venterPåHendelse(any());
    }

    @Test
    public void testSkalBehandleKvittering() {
        // Arrange
        when(prosessTaskData.getHendelse()).thenReturn(Optional.of(ProsessTaskHendelse.ØKONOMI_OPPDRAG_KVITTERING));

        // Act
        task.doTask(prosessTaskData);

        // Assert
        verify(oppdragskontrollTjeneste, never()).opprettOppdrag(anyLong(), anyLong());
        verify(prosessTaskData, never()).venterPåHendelse(any());
        verify(repo, never()).lagre(any(ProsessTaskData.class));
    }
}
