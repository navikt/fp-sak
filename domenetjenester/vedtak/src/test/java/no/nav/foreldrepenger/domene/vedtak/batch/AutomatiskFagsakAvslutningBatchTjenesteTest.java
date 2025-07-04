package no.nav.foreldrepenger.domene.vedtak.batch;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;

class AutomatiskFagsakAvslutningBatchTjenesteTest {

    private AutomatiskFagsakAvslutningBatchTjeneste tjeneste;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private AutomatiskFagsakAvslutningTjeneste fagsakAvslutningTjeneste;


    @BeforeEach
    void setUp() {
        behandlingRepository = Mockito.mock(BehandlingRepository.class);
        fagsakRepository = Mockito.mock(FagsakRepository.class);
        fagsakAvslutningTjeneste = Mockito.mock(AutomatiskFagsakAvslutningTjeneste.class);
        tjeneste = new AutomatiskFagsakAvslutningBatchTjeneste(fagsakAvslutningTjeneste);
    }

    @Test
    void skal_kjøre_batch_uten_feil() {
        var fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        fagsak1.setId(1L);
        var fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        fagsak2.setId(2L);
        var behandling = Mockito.mock(Behandling.class);
        Mockito.when(behandling.getId()).thenReturn(3L);
        Mockito.when(behandling.getFagsakId()).thenReturn(2L);
        Mockito.when(behandling.getAktørId()).thenReturn(AktørId.dummy());
        Mockito.when(fagsakRepository.hentForStatus(FagsakStatus.LØPENDE)).thenReturn(Arrays.asList(fagsak1, fagsak2));
        Mockito.when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(1L)).thenReturn(Optional.empty());
        Mockito.when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(2L)).thenReturn(Optional.of(behandling));

        var batchId = tjeneste.launch(new Properties());

        Mockito.verify(fagsakAvslutningTjeneste, Mockito.times(1)).avsluttFagsaker("BVL006", LocalDate.now());
        Assertions.assertThat(batchId.substring(0, 6)).isEqualTo("BVL006");
    }
}
