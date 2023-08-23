package no.nav.foreldrepenger.domene.vedtak.batch;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutomatiskFagsakAvslutningTjenesteTest {

    private AutomatiskFagsakAvslutningTjeneste automatiskFagsakAvslutningTjeneste;

    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    @Mock
    private FagsakRelasjonRepository fagsakRelasjonRepository;

    @BeforeEach
    public void setUp() {
        taskTjeneste = spy(ProsessTaskTjeneste.class);
        automatiskFagsakAvslutningTjeneste = new AutomatiskFagsakAvslutningTjeneste(taskTjeneste, fagsakRelasjonRepository);
    }

    @Test
    void ingen_fagsak_avslutning() {
        when(fagsakRelasjonRepository.finnFagsakerForAvsluttning(LocalDate.now())).thenReturn(List.of());

        automatiskFagsakAvslutningTjeneste.avsluttFagsaker("", LocalDate.now());
        Mockito.verifyNoInteractions(taskTjeneste);
    }

    @Test
    void en_fagsak_avslutning() {
        var fagsak = lagFagsak(FagsakStatus.LØPENDE);
        when(fagsakRelasjonRepository.finnFagsakerForAvsluttning(LocalDate.now())).thenReturn(List.of(fagsak));

        automatiskFagsakAvslutningTjeneste.avsluttFagsaker("", LocalDate.now());
        Mockito.verify(taskTjeneste, Mockito.times(1)).lagre(Mockito.any(ProsessTaskData.class));
    }

    @Test
    void en_fagsak_avslutning_med_berørt_sak() {
        var fagsak = lagFagsak(FagsakStatus.LØPENDE);
        when(fagsakRelasjonRepository.finnFagsakerForAvsluttning(LocalDate.now())).thenReturn(List.of(fagsak));

        automatiskFagsakAvslutningTjeneste.avsluttFagsaker("", LocalDate.now());
        Mockito.verify(taskTjeneste, Mockito.times(1)).lagre(Mockito.any(ProsessTaskData.class));
    }

    private Fagsak lagFagsak(FagsakStatus fagsakStatus) {
        var fagsak = mock(Fagsak.class);
        // when(fagsak.getStatus()).thenReturn(fagsakStatus);
        var aktørId = mock(AktørId.class);
        when(aktørId.getId()).thenReturn("1");
        when(fagsak.getAktørId()).thenReturn(aktørId);

        return fagsak;
    }

}
