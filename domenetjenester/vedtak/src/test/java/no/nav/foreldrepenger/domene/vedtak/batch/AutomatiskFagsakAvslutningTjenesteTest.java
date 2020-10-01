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

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ExtendWith(MockitoExtension.class)
public class AutomatiskFagsakAvslutningTjenesteTest {

    private AutomatiskFagsakAvslutningTjeneste automatiskFagsakAvslutningTjeneste;

    @Mock
    private ProsessTaskRepository prosessTaskRepository;
    @Mock
    private FagsakRelasjonRepository fagsakRelasjonRepository;

    @BeforeEach
    public void setUp() {
        prosessTaskRepository = spy(ProsessTaskRepository.class);
        automatiskFagsakAvslutningTjeneste = new AutomatiskFagsakAvslutningTjeneste(prosessTaskRepository, fagsakRelasjonRepository);
    }

    @Test
    public void ingen_fagsak_avslutning() {
        when(fagsakRelasjonRepository.finnFagsakerForAvsluttning(LocalDate.now())).thenReturn(List.of());

        automatiskFagsakAvslutningTjeneste.avsluttFagsaker("", LocalDate.now());
        Mockito.verify(prosessTaskRepository, Mockito.times(0)).lagre(Mockito.any(ProsessTaskGruppe.class));
    }

    @Test
    public void en_fagsak_avslutning() {
        Fagsak fagsak = lagFagsak(FagsakStatus.LØPENDE);
        when(fagsakRelasjonRepository.finnFagsakerForAvsluttning(LocalDate.now())).thenReturn(List.of(fagsak));

        automatiskFagsakAvslutningTjeneste.avsluttFagsaker("", LocalDate.now());
        Mockito.verify(prosessTaskRepository, Mockito.times(1)).lagre(Mockito.any(ProsessTaskGruppe.class));
    }

    @Test
    public void en_fagsak_avslutning_med_berørt_sak() {
        Fagsak fagsak = lagFagsak(FagsakStatus.LØPENDE);
        when(fagsakRelasjonRepository.finnFagsakerForAvsluttning(LocalDate.now())).thenReturn(List.of(fagsak));

        automatiskFagsakAvslutningTjeneste.avsluttFagsaker("", LocalDate.now());
        Mockito.verify(prosessTaskRepository, Mockito.times(1)).lagre(Mockito.any(ProsessTaskGruppe.class));
    }

    private Fagsak lagFagsak(FagsakStatus fagsakStatus) {
        Fagsak fagsak = mock(Fagsak.class);
        // when(fagsak.getStatus()).thenReturn(fagsakStatus);
        AktørId aktørId = mock(AktørId.class);
        when(aktørId.getId()).thenReturn("1");
        when(fagsak.getAktørId()).thenReturn(aktørId);

        return fagsak;
    }

}
