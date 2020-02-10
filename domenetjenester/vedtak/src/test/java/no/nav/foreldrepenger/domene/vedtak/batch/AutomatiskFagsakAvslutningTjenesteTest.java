package no.nav.foreldrepenger.domene.vedtak.batch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class AutomatiskFagsakAvslutningTjenesteTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private AutomatiskFagsakAvslutningTjeneste automatiskFagsakAvslutningTjeneste;

    @Mock
    private ProsessTaskRepository prosessTaskRepository;
    @Mock
    private FagsakRelasjonRepository fagsakRelasjonRepository = mock(FagsakRelasjonRepository.class);

    @Before
    public void setUp() {
        prosessTaskRepository = spy(ProsessTaskRepository.class);
        automatiskFagsakAvslutningTjeneste = new AutomatiskFagsakAvslutningTjeneste(prosessTaskRepository, fagsakRelasjonRepository);
    }

    @Test
    public void ingen_fagsak_avslutning() {
        when(fagsakRelasjonRepository.finnRelasjonerForAvsluttningAvFagsaker(LocalDate.now(),0)).thenReturn(List.of());

        automatiskFagsakAvslutningTjeneste.avsluttFagsaker("", LocalDate.now(),0);
        Mockito.verify(prosessTaskRepository, Mockito.times(0)).lagre(Mockito.any(ProsessTaskGruppe.class));
    }

    @Test
    public void en_fagsak_avslutning() {
        FagsakRelasjon fagsakRelasjon = lagFagsakRelasjon(FagsakStatus.LØPENDE, false, FagsakStatus.DEFAULT);
        when(fagsakRelasjonRepository.finnRelasjonerForAvsluttningAvFagsaker(LocalDate.now(),0)).thenReturn(List.of(fagsakRelasjon));

        automatiskFagsakAvslutningTjeneste.avsluttFagsaker("", LocalDate.now(),0);
        Mockito.verify(prosessTaskRepository, Mockito.times(1)).lagre(Mockito.any(ProsessTaskGruppe.class));
    }

    @Test
    public void en_fagsak_avslutning_allerede_avsluttet() {
        FagsakRelasjon fagsakRelasjon = lagFagsakRelasjon(FagsakStatus.AVSLUTTET, false, FagsakStatus.DEFAULT);
        when(fagsakRelasjonRepository.finnRelasjonerForAvsluttningAvFagsaker(LocalDate.now(),0)).thenReturn(List.of(fagsakRelasjon));

        automatiskFagsakAvslutningTjeneste.avsluttFagsaker("", LocalDate.now(),0);
        Mockito.verify(prosessTaskRepository, Mockito.times(0)).lagre(Mockito.any(ProsessTaskGruppe.class));
    }

    @Test
    public void en_fagsak_avslutning_med_berørt_sak() {
        FagsakRelasjon fagsakRelasjon = lagFagsakRelasjon(FagsakStatus.LØPENDE, true, FagsakStatus.LØPENDE);
        when(fagsakRelasjonRepository.finnRelasjonerForAvsluttningAvFagsaker(LocalDate.now(),0)).thenReturn(List.of(fagsakRelasjon));

        automatiskFagsakAvslutningTjeneste.avsluttFagsaker("", LocalDate.now(),0);
        Mockito.verify(prosessTaskRepository, Mockito.times(1)).lagre(Mockito.any(ProsessTaskGruppe.class));
    }

    @Test
    public void en_fagsak_avslutning_med_berørt_sak_som_allerede_er_avsluttet() {
        FagsakRelasjon fagsakRelasjon = lagFagsakRelasjon(FagsakStatus.LØPENDE, true, FagsakStatus.AVSLUTTET);
        when(fagsakRelasjonRepository.finnRelasjonerForAvsluttningAvFagsaker(LocalDate.now(),0)).thenReturn(List.of(fagsakRelasjon));

        automatiskFagsakAvslutningTjeneste.avsluttFagsaker("", LocalDate.now(),0);
        Mockito.verify(prosessTaskRepository, Mockito.times(1)).lagre(Mockito.any(ProsessTaskGruppe.class));
    }

    private FagsakRelasjon lagFagsakRelasjon(FagsakStatus fagsakStatus, boolean medFagsakTo, FagsakStatus fagsakStatus2) {
        FagsakRelasjon fagsakRelasjon = mock(FagsakRelasjon.class);
        Fagsak fagsak1 = mock(Fagsak.class);
        AktørId aktørId = mock(AktørId.class);
        when(aktørId.getId()).thenReturn("1");
        when(fagsak1.getStatus()).thenReturn(fagsakStatus);
        when(fagsak1.getId()).thenReturn(1L);
        when(fagsak1.getAktørId()).thenReturn(aktørId);
        when(fagsakRelasjon.getFagsakNrEn()).thenReturn(fagsak1);
        if (medFagsakTo) {
            Fagsak fagsak2 = mock(Fagsak.class);
            AktørId aktørId2 = mock(AktørId.class);
            when(aktørId2.getId()).thenReturn("2");
            when(fagsak2.getStatus()).thenReturn(fagsakStatus2);
            when(fagsak2.getId()).thenReturn(2L);
            when(fagsak2.getAktørId()).thenReturn(aktørId2);
            when(fagsakRelasjon.getFagsakNrTo()).thenReturn(Optional.of(fagsak2));
        } else {
            when(fagsakRelasjon.getFagsakNrTo()).thenReturn(Optional.empty());
        }

        return fagsakRelasjon;
    }
}
