package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

class OpprettOppgaveVurderKonsekvensTaskTest {

    private static final long FAGSAK_ID = 2L;
    private static final String SAKSNUMMER = "2";
    private OppgaveTjeneste oppgaveTjeneste;
    private OpprettOppgaveVurderKonsekvensTask opprettOppgaveVurderKonsekvensTask;
    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakLåsRepository låsRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    @BeforeEach
    void before() {
        oppgaveTjeneste = mock(OppgaveTjeneste.class);
        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        låsRepository = mock(FagsakLåsRepository.class);
        behandlendeEnhetTjeneste = mock(BehandlendeEnhetTjeneste.class);

        when(repositoryProvider.getFagsakLåsRepository()).thenReturn(låsRepository);
        when(låsRepository.taLås(anyLong())).thenReturn(mock(FagsakLås.class));
        lenient().when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(), any(String.class))).thenReturn(new OrganisasjonsEnhet("1234", "NFP"));

        opprettOppgaveVurderKonsekvensTask = new OpprettOppgaveVurderKonsekvensTask(oppgaveTjeneste, behandlendeEnhetTjeneste);
    }

    @Test
    void skal_opprette_oppgave_for_å_vurdere_konsekvens_basert_på_fagsakId() {
        // Arrange
        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class);
        prosessTaskData.setFagsak(SAKSNUMMER, FAGSAK_ID);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, OpprettOppgaveVurderKonsekvensTask.STANDARD_BESKRIVELSE);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_NORM);
        var fagsakIdCaptor = ArgumentCaptor.forClass(Long.class);
        var beskrivelseCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        opprettOppgaveVurderKonsekvensTask.doTask(prosessTaskData);

        // Assert
        verify(oppgaveTjeneste).opprettVurderKonsekvensBasertPåFagsakId(fagsakIdCaptor.capture(), any(),
                beskrivelseCaptor.capture(), Mockito.eq(false));
        assertThat(fagsakIdCaptor.getValue()).isEqualTo(FAGSAK_ID);
        assertThat(beskrivelseCaptor.getValue()).isEqualTo(OpprettOppgaveVurderKonsekvensTask.STANDARD_BESKRIVELSE);
    }
}
