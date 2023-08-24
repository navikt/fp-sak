package no.nav.foreldrepenger.web.app.soap.sak.tjeneste;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Journalpost;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpprettSakOrchestratorTest {

    private static final FagsakYtelseType EXPECTED_YTELSE_TYPE = FagsakYtelseType.FORELDREPENGER;
    private static final AktørId EXPECTED_AKTØR_ID = AktørId.dummy();
    private static final Saksnummer EXPECTED_SAKSNUMMER = new Saksnummer("23424243");
    private static final Fagsak FAGSAK = Fagsak.opprettNy(EXPECTED_YTELSE_TYPE, NavBruker.opprettNyNB(EXPECTED_AKTØR_ID), EXPECTED_SAKSNUMMER);
    private  static final JournalpostId EXPECTED_JOURNALPOST_ID = new JournalpostId("1234567");

    @Mock
    private OpprettSakTjeneste opprettSakTjeneste;
    @Mock
    private FagsakRepository fagsakRepository;

    private OpprettSakOrchestrator tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new OpprettSakOrchestrator(opprettSakTjeneste, fagsakRepository);
    }

    @Test
    void skal_opprette_en_ny_sak_om_journalpostId_er_null() {

        when(opprettSakTjeneste.opprettSakVL(EXPECTED_AKTØR_ID, EXPECTED_YTELSE_TYPE)).thenReturn(FAGSAK);

        var saksnummer = tjeneste.opprettSak(EXPECTED_YTELSE_TYPE, EXPECTED_AKTØR_ID, null);

        Mockito.verify(opprettSakTjeneste, Mockito.times(1)).opprettSakVL(EXPECTED_AKTØR_ID, EXPECTED_YTELSE_TYPE);
        Mockito.verifyNoInteractions(fagsakRepository);
        assertThat(saksnummer).isNotNull().isEqualTo(EXPECTED_SAKSNUMMER);
    }

    @Test
    void skal_returnere_sak_koblet_til_journalpostId_hvis_finnes() {

        var journalpostMock = mock(Journalpost.class);
        when(journalpostMock.getFagsak()).thenReturn(FAGSAK);
        when(fagsakRepository.hentJournalpost(EXPECTED_JOURNALPOST_ID)).thenReturn(Optional.of(journalpostMock));

        // test
        var saksnummer = tjeneste.opprettSak(EXPECTED_YTELSE_TYPE, EXPECTED_AKTØR_ID, EXPECTED_JOURNALPOST_ID);

        Mockito.verify(fagsakRepository, Mockito.times(1)).hentJournalpost(EXPECTED_JOURNALPOST_ID);
        Mockito.verifyNoInteractions(opprettSakTjeneste);
        assertThat(saksnummer).isNotNull().isEqualTo(EXPECTED_SAKSNUMMER);
    }

    @Test
    void skal_opprette_en_ny_sak_om_ingen_sak_er_koblet_til_journalpostId() {
        when(opprettSakTjeneste.opprettSakVL(EXPECTED_AKTØR_ID, EXPECTED_YTELSE_TYPE, EXPECTED_JOURNALPOST_ID)).thenReturn(FAGSAK);
        when(fagsakRepository.hentJournalpost(EXPECTED_JOURNALPOST_ID)).thenReturn(Optional.empty());

        // test
        var saksnummer = tjeneste.opprettSak(EXPECTED_YTELSE_TYPE, EXPECTED_AKTØR_ID, EXPECTED_JOURNALPOST_ID);

        Mockito.verify(fagsakRepository, Mockito.times(1)).hentJournalpost(EXPECTED_JOURNALPOST_ID);
        Mockito.verify(opprettSakTjeneste, times(1)).opprettSakVL(EXPECTED_AKTØR_ID, EXPECTED_YTELSE_TYPE, EXPECTED_JOURNALPOST_ID);
        assertThat(saksnummer).isNotNull().isEqualTo(EXPECTED_SAKSNUMMER);
    }
}
