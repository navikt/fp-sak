package no.nav.foreldrepenger.domene.vedtak.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import no.nav.foreldrepenger.batch.BatchStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.TaskStatus;

public class AutomatiskFagsakAvslutningBatchTjenesteTest {

    private AutomatiskFagsakAvslutningBatchTjeneste tjeneste;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private AutomatiskFagsakAvslutningTjeneste fagsakAvslutningTjeneste;


    @Before
    public void setUp() {
        behandlingRepository = Mockito.mock(BehandlingRepository.class);
        fagsakRepository = Mockito.mock(FagsakRepository.class);
        fagsakAvslutningTjeneste = Mockito.mock(AutomatiskFagsakAvslutningTjeneste.class);
        tjeneste = new AutomatiskFagsakAvslutningBatchTjeneste(fagsakAvslutningTjeneste);
    }

    @Test
    public void skal_returnere_status_ok_ved_fullført() {
        final List<TaskStatus> statuses = List.of(new TaskStatus(ProsessTaskStatus.FERDIG, BigDecimal.ONE));
        Mockito.when(fagsakAvslutningTjeneste.hentStatusForFagsakAvslutningGruppe(ArgumentMatchers.anyString())).thenReturn(statuses);

        final BatchStatus status = tjeneste.status("1234");

        Mockito.verify(fagsakAvslutningTjeneste).hentStatusForFagsakAvslutningGruppe("1234");
        assertThat(status).isEqualTo(BatchStatus.OK);
    }

    @Test
    public void skal_kjøre_batch_uten_feil() {
        Fagsak fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        fagsak1.setId(1L);
        Fagsak fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        fagsak2.setId(2L);
        Behandling behandling = Mockito.mock(Behandling.class);
        Mockito.when(behandling.getId()).thenReturn(3L);
        Mockito.when(behandling.getFagsakId()).thenReturn(2L);
        Mockito.when(behandling.getAktørId()).thenReturn(AktørId.dummy());
        Mockito.when(fagsakRepository.hentForStatus(FagsakStatus.LØPENDE)).thenReturn(Arrays.asList(fagsak1, fagsak2));
        Mockito.when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(1L)).thenReturn(Optional.empty());
        Mockito.when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(2L)).thenReturn(Optional.of(behandling));

        final String batchId = tjeneste.launch(null);

        Mockito.verify(fagsakAvslutningTjeneste, Mockito.times(1)).avsluttFagsaker("BVL006", LocalDate.now());
        Assertions.assertThat(batchId.substring(0, 6)).isEqualTo("BVL006");
    }
}
