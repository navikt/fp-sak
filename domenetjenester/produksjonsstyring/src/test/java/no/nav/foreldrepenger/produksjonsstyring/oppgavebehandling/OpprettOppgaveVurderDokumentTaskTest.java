package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderDokumentTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

class OpprettOppgaveVurderDokumentTaskTest {

    private static final long FAGSAK_ID = 2L;
    private static final String SAKSNUMMER = "2";

    private OppgaveTjeneste oppgaveTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;
    private OpprettOppgaveVurderDokumentTask opprettOppgaveVurderDokumentTask;
    private FagsakLåsRepository låsRepository;

    @BeforeEach
    public void before() {
        oppgaveTjeneste = mock(OppgaveTjeneste.class);
        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        låsRepository = mock(FagsakLåsRepository.class);

        when(repositoryProvider.getFagsakLåsRepository()).thenReturn(låsRepository);
        when(låsRepository.taLås(anyLong())).thenReturn(mock(FagsakLås.class));

        opprettOppgaveVurderDokumentTask = new OpprettOppgaveVurderDokumentTask(oppgaveTjeneste);
    }

    @Test
    void skal_opprette_oppgave_for_å_vurdere_dokument_basert_på_fagsakId() {
        // Arrange
        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettOppgaveVurderDokumentTask.class);
        prosessTaskData.setFagsak(SAKSNUMMER, FAGSAK_ID);
        prosessTaskData.setProperty(OpprettOppgaveVurderDokumentTask.KEY_DOKUMENT_TYPE, DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL.getKode());
        var fagsakIdCaptor = ArgumentCaptor.forClass(Long.class);
        var fordelingsoppgaveEnhetsIdCaptor = ArgumentCaptor.forClass(String.class);
        var beskrivelseCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        opprettOppgaveVurderDokumentTask.doTask(prosessTaskData);

        // Assert
        verify(oppgaveTjeneste).opprettVurderDokumentMedBeskrivelseBasertPåFagsakId(fagsakIdCaptor.capture(), any(), fordelingsoppgaveEnhetsIdCaptor.capture(), beskrivelseCaptor.capture());
        assertThat(fagsakIdCaptor.getValue()).isEqualTo(FAGSAK_ID);
        var dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;
        assertThat(beskrivelseCaptor.getValue()).isEqualTo("VL: " + dokumentTypeId.getNavn()); // Antar testhelper, ellers bruk finn+navn
    }
}
