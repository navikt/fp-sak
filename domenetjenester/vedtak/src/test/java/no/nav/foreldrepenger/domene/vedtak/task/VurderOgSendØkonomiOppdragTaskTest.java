package no.nav.foreldrepenger.domene.vedtak.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.OppdragInputTjeneste;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollEngangsstønadTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.postcondition.OppdragPostConditionTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.tjeneste.NyOppdragskontrollTjenesteImpl;
import no.nav.foreldrepenger.økonomistøtte.ny.toggle.OppdragKjerneimplementasjonToggle;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ExtendWith(MockitoExtension.class)
public class VurderOgSendØkonomiOppdragTaskTest {

    private static final Long BEHANDLING_ID = 139L;

    private static final Long TASK_ID = 238L;

    private static final String AKTØR_ID = AktørId.dummy().getId();

    @Mock
    private ProsessTaskRepository repo;

    @Mock
    private ProsessTaskData prosessTaskData;

    @Mock
    private OppdragskontrollTjeneste oppdragskontrollTjeneste;

    @Mock
    private OppdragskontrollTjeneste nyOppdragskontrollTjeneste;

    @Mock
    private OppdragInputTjeneste oppdragInputTjeneste;

    @Mock
    private OppdragPostConditionTjeneste oppdragPostConditionTjeneste;

    @Mock
    private OppdragKjerneimplementasjonToggle toggle;

    private VurderOgSendØkonomiOppdragTask task;

    @BeforeEach
    public void setUp() {
        when(prosessTaskData.getBehandlingId()).thenReturn(BEHANDLING_ID.toString());
        lenient().when(prosessTaskData.getId()).thenReturn(TASK_ID);
        lenient().when(prosessTaskData.getAktørId()).thenReturn(AKTØR_ID);
        var repositoryProvider = ScenarioMorSøkerForeldrepenger.forFødsel().mockBehandlingRepositoryProvider();
        lenient().when(repositoryProvider.getBehandlingRepository().hentBehandling(BEHANDLING_ID)).thenReturn(Behandling.nyBehandlingFor(Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNy(AktørId.dummy(), Språkkode.NB)), BehandlingType.FØRSTEGANGSSØKNAD).build());
        task = new VurderOgSendØkonomiOppdragTask(oppdragskontrollTjeneste, oppdragskontrollTjeneste, repo, repositoryProvider, nyOppdragskontrollTjeneste, oppdragPostConditionTjeneste, toggle, oppdragInputTjeneste);
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
        when(oppdragskontrollTjeneste.opprettOppdrag(anyLong(), anyLong())).thenReturn(
            Optional.ofNullable(oppdragskontroll));
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
