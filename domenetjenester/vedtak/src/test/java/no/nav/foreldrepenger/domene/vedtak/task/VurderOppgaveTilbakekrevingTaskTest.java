package no.nav.foreldrepenger.domene.vedtak.task;

import no.finn.unleash.Unleash;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class VurderOppgaveTilbakekrevingTaskTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private TilbakekrevingRepository tilbakekrevingRepository = new TilbakekrevingRepository(repoRule.getEntityManager());

    private VurderOppgaveTilbakekrevingTask vurderOppgaveTilbakekrevingTask;
    private OppgaveTjeneste oppgaveTjeneste;

    private Unleash unleash;

    @Before
    public void setUp() {
        unleash = mock(Unleash.class);
        oppgaveTjeneste = mock(OppgaveTjeneste.class);
        when(unleash.isEnabled(anyString(), anyBoolean())).thenReturn(true);

        vurderOppgaveTilbakekrevingTask = new VurderOppgaveTilbakekrevingTask(oppgaveTjeneste, repositoryProvider, tilbakekrevingRepository);
    }

    @Test
    public void skal_opprette_tilbakekrevingsoppgave() {
        // Arrange
        Behandling behandling = opprettBehandlingMedTilbakekreving(BehandlingType.REVURDERING, TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD);

        ProsessTaskData prosessTaskData = new ProsessTaskData(VurderOppgaveTilbakekrevingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        // Act
        vurderOppgaveTilbakekrevingTask.doTask(prosessTaskData);

        // Assert
        verify(oppgaveTjeneste).opprettOppgaveFeilutbetaling(eq(behandling.getId()), anyString());
    }

    @Test
    public void skal_ikke_opprette_tilbakekrevingsoppgave_hvis_viderebehandling_ignorer() {
        // Arrange
        Behandling behandling = opprettBehandlingMedTilbakekreving(BehandlingType.REVURDERING, TilbakekrevingVidereBehandling.IGNORER_TILBAKEKREVING);

        ProsessTaskData prosessTaskData = new ProsessTaskData(VurderOppgaveTilbakekrevingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        // Act
        vurderOppgaveTilbakekrevingTask.doTask(prosessTaskData);

        // Assert
        verify(oppgaveTjeneste, never()).opprettOppgaveFeilutbetaling(anyLong(), anyString());
    }

    @Test
    public void skal_ikke_opprette_tilbakekrevingsoppgave_hvis_viderebehandling_inntrekk() {
        // Arrange
        Behandling behandling = opprettBehandlingMedInntrekk(BehandlingType.REVURDERING, TilbakekrevingVidereBehandling.INNTREKK, true, false);

        ProsessTaskData prosessTaskData = new ProsessTaskData(VurderOppgaveTilbakekrevingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        // Act
        vurderOppgaveTilbakekrevingTask.doTask(prosessTaskData);

        // Assert
        verify(oppgaveTjeneste, never()).opprettOppgaveFeilutbetaling(anyLong(), anyString());
    }

    @Test
    public void skal_ikke_opprette_tilbakekrevingsoppgave_hvis_viderebehandling_udefinert() {
        // Arrange
        Behandling behandling = opprettBehandlingMedTilbakekreving(BehandlingType.REVURDERING, TilbakekrevingVidereBehandling.UDEFINIERT);

        ProsessTaskData prosessTaskData = new ProsessTaskData(VurderOppgaveTilbakekrevingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        // Act
        vurderOppgaveTilbakekrevingTask.doTask(prosessTaskData);

        // Assert
        verify(oppgaveTjeneste, never()).opprettOppgaveFeilutbetaling(anyLong(), anyString());
    }

    @Test
    public void skal_ikke_opprette_tilbakekrevingsoppgave_hvis_tilbakekrevingvalg_ikke_finnes() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        Behandling behandling = scenario.lagre(repositoryProvider);
        repoRule.getRepository().flushAndClear();

        ProsessTaskData prosessTaskData = new ProsessTaskData(VurderOppgaveTilbakekrevingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        // Act
        vurderOppgaveTilbakekrevingTask.doTask(prosessTaskData);

        // Assert
        verify(oppgaveTjeneste, never()).opprettOppgaveFeilutbetaling(anyLong(), anyString());
    }

    private Behandling opprettBehandlingMedTilbakekreving(BehandlingType behandlingType, TilbakekrevingVidereBehandling tilbakekrevingVidereBehandling) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(behandlingType);
        Behandling behandling = scenario.lagre(repositoryProvider);
        tilbakekrevingRepository.lagre(behandling, TilbakekrevingValg.utenMulighetForInntrekk(tilbakekrevingVidereBehandling, "varseltekst"));
        repoRule.getRepository().flushAndClear();
        return behandling;
    }

    private Behandling opprettBehandlingMedInntrekk(BehandlingType behandlingType, TilbakekrevingVidereBehandling tilbakekrevingVidereBehandling,
                                                    boolean oppfylt, boolean reduksjon) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(behandlingType);
        Behandling behandling = scenario.lagre(repositoryProvider);
        tilbakekrevingRepository.lagre(behandling, TilbakekrevingValg.medMulighetForInntrekk(oppfylt, reduksjon, tilbakekrevingVidereBehandling));
        repoRule.getRepository().flushAndClear();
        return behandling;
    }
}
