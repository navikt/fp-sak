package no.nav.foreldrepenger.domene.vedtak.batch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(MockitoExtension.class)
class AutomatiskFagsakAvslutningTjenesteTest {

    private AutomatiskFagsakAvslutningTjeneste automatiskFagsakAvslutningTjeneste;

    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    @Mock
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    @BeforeEach
    public void setUp() {
        taskTjeneste = spy(ProsessTaskTjeneste.class);
        automatiskFagsakAvslutningTjeneste = new AutomatiskFagsakAvslutningTjeneste(taskTjeneste, fagsakRelasjonTjeneste);
    }

    @Test
    void ingen_fagsak_avslutning() {
        when(fagsakRelasjonTjeneste.finnFagsakerForAvsluttning(LocalDate.now())).thenReturn(List.of());

        automatiskFagsakAvslutningTjeneste.avsluttFagsaker("", LocalDate.now());
        Mockito.verifyNoInteractions(taskTjeneste);
    }

    @Test
    void en_fagsak_avslutning() {
        var fagsak = lagFagsak();
        when(fagsakRelasjonTjeneste.finnFagsakerForAvsluttning(LocalDate.now())).thenReturn(List.of(fagsak));

        automatiskFagsakAvslutningTjeneste.avsluttFagsaker("", LocalDate.now());
        Mockito.verify(taskTjeneste, Mockito.times(1)).lagre(Mockito.any(ProsessTaskData.class));
    }

    @Test
    void en_fagsak_avslutning_med_berørt_sak() {
        var fagsak = lagFagsak();
        when(fagsakRelasjonTjeneste.finnFagsakerForAvsluttning(LocalDate.now())).thenReturn(List.of(fagsak));

        automatiskFagsakAvslutningTjeneste.avsluttFagsaker("", LocalDate.now());
        Mockito.verify(taskTjeneste, Mockito.times(1)).lagre(Mockito.any(ProsessTaskData.class));
    }

    private Fagsak lagFagsak() {
        var fagsak = mock(Fagsak.class);
        var saksnummer = new Saksnummer("9999");
        when(fagsak.getSaksnummer()).thenReturn(saksnummer);

        return fagsak;
    }

}
