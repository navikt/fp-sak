package no.nav.foreldrepenger.domene.vedtak.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.BehandleØkonomioppdragKvittering;
import no.nav.foreldrepenger.økonomistøtte.OppdragInputTjeneste;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.postcondition.OppdragPostConditionTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(MockitoExtension.class)
class VurderOgSendØkonomiOppdragTaskTest {

    private static final Long BEHANDLING_ID = 139L;
    private static final String SAKSNUMMER = "9999";

    private static final Long TASK_ID = 238L;

    @Mock
    private ProsessTaskTjeneste repo;

    @Mock
    private ProsessTaskData prosessTaskData;

    @Mock
    private OppdragskontrollTjeneste nyOppdragskontrollTjeneste;

    @Mock
    private OppdragInputTjeneste oppdragInputTjeneste;

    @Mock
    private OppdragPostConditionTjeneste oppdragPostConditionTjeneste;

    private VurderOgSendØkonomiOppdragTask task;

    @BeforeEach
    public void setUp() {
        when(prosessTaskData.getBehandlingIdAsLong()).thenReturn(BEHANDLING_ID);
        lenient().when(prosessTaskData.getSaksnummer()).thenReturn(SAKSNUMMER);
        lenient().when(prosessTaskData.getId()).thenReturn(TASK_ID);
        var repositoryProvider = ScenarioMorSøkerForeldrepenger.forFødsel().mockBehandlingRepositoryProvider();
        lenient().when(repositoryProvider.getBehandlingRepository().hentBehandling(BEHANDLING_ID))
            .thenReturn(Behandling.nyBehandlingFor(Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AktørId.dummy(), Språkkode.NB)), BehandlingType.FØRSTEGANGSSØKNAD).build());
        task = new VurderOgSendØkonomiOppdragTask(repo, repositoryProvider, nyOppdragskontrollTjeneste, oppdragPostConditionTjeneste, oppdragInputTjeneste);
    }

    @Test
    void testSkalSendeOppdrag() {
        // Arrange
        var oppdragskontroll = Oppdragskontroll.builder()
            .medBehandlingId(BEHANDLING_ID)
            .medProsessTaskId(1L)
            .medVenterKvittering(true)
            .medSaksnummer(new Saksnummer(BEHANDLING_ID.toString()))
            .build();
        when(nyOppdragskontrollTjeneste.opprettOppdrag(any())).thenReturn(
            Optional.ofNullable(oppdragskontroll));
        when(prosessTaskData.getVentetHendelse()).thenReturn(Optional.empty());

        // Act
        task.doTask(prosessTaskData);

        // Assert
        verify(prosessTaskData).venterPåHendelse(BehandleØkonomioppdragKvittering.ØKONOMI_OPPDRAG_KVITTERING);
        verify(repo).lagre(prosessTaskData);
    }

    @Test
    void testSkalIkkeSendeOppdrag() {
        // Arrange
        when(prosessTaskData.getVentetHendelse()).thenReturn(Optional.empty());

        // Act
        task.doTask(prosessTaskData);

        // Assert oppretter bare prosesstask for å sende tilkjent ytelse
        verifyNoInteractions(repo);

        verify(prosessTaskData, never()).venterPåHendelse(any());
    }

    @Test
    void testSkalBehandleKvittering() {
        // Arrange
        when(prosessTaskData.getVentetHendelse()).thenReturn(Optional.of(BehandleØkonomioppdragKvittering.ØKONOMI_OPPDRAG_KVITTERING));

        // Act
        task.doTask(prosessTaskData);

        // Assert
        verify(nyOppdragskontrollTjeneste, never()).opprettOppdrag(any());
        verify(prosessTaskData, never()).venterPåHendelse(any());
        verify(repo, never()).lagre(any(ProsessTaskData.class));
    }
}
