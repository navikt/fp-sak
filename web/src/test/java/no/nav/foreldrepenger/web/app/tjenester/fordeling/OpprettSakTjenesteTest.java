package no.nav.foreldrepenger.web.app.tjenester.fordeling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import no.nav.vedtak.exception.TekniskException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Journalpost;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ExtendWith(MockitoExtension.class)
class OpprettSakTjenesteTest {

    private static final FagsakYtelseType EXPECTED_YTELSE_TYPE = FagsakYtelseType.FORELDREPENGER;
    private static final AktørId EXPECTED_AKTØR_ID = AktørId.dummy();
    protected static final NavBruker NAV_BRUKER = NavBruker.opprettNy(EXPECTED_AKTØR_ID, Språkkode.NB);
    private static final Saksnummer EXPECTED_SAKSNUMMER = new Saksnummer("23424243");
    private static final Fagsak FAGSAK = Fagsak.opprettNy(EXPECTED_YTELSE_TYPE, NavBruker.opprettNyNB(EXPECTED_AKTØR_ID), EXPECTED_SAKSNUMMER);
    private  static final JournalpostId EXPECTED_JOURNALPOST_ID = new JournalpostId("1234567");

    @Mock
    private NavBrukerTjeneste brukerTjeneste;
    @Mock
    private FagsakTjeneste fagsakTjeneste;

    private OpprettSakTjeneste opprettSakTjeneste;

    @BeforeEach
    void setUp() {
        this.opprettSakTjeneste = new OpprettSakTjeneste(fagsakTjeneste, brukerTjeneste);
    }

    @Test
    void skal_opprette_en_ny_sak_om_journalpostId_er_null() {
        when(brukerTjeneste.hentEllerOpprettFraAktørId(any(AktørId.class))).thenReturn(NAV_BRUKER);
        when(fagsakTjeneste.opprettFagsak(any(), eq(NAV_BRUKER))).thenReturn(FAGSAK);

        var saksnummer = opprettSakTjeneste.opprettSak(EXPECTED_YTELSE_TYPE, EXPECTED_AKTØR_ID, null);

        verify(fagsakTjeneste).opprettFagsak(EXPECTED_YTELSE_TYPE, NAV_BRUKER);
        assertThat(saksnummer).isNotNull().isEqualTo(EXPECTED_SAKSNUMMER);
    }

    @Test
    void skal_returnere_sak_koblet_til_journalpostId_hvis_finnes() {
        var journalpostMock = mock(Journalpost.class);
        when(journalpostMock.getFagsak()).thenReturn(FAGSAK);
        when(fagsakTjeneste.hentJournalpost(EXPECTED_JOURNALPOST_ID)).thenReturn(Optional.of(journalpostMock));

        // test
        var saksnummer = opprettSakTjeneste.opprettSak(EXPECTED_YTELSE_TYPE, EXPECTED_AKTØR_ID, EXPECTED_JOURNALPOST_ID);

        verify(fagsakTjeneste).hentJournalpost(EXPECTED_JOURNALPOST_ID);
        verifyNoMoreInteractions(fagsakTjeneste);

        assertThat(saksnummer).isNotNull().isEqualTo(EXPECTED_SAKSNUMMER);
    }

    @Test
    void skal_opprette_en_ny_sak_om_ingen_sak_er_koblet_til_journalpostId() {
        when(brukerTjeneste.hentEllerOpprettFraAktørId(any(AktørId.class))).thenReturn(NAV_BRUKER);

        when(fagsakTjeneste.opprettFagsak(any(), eq(NAV_BRUKER))).thenReturn(FAGSAK);
        when(fagsakTjeneste.finnFagsakGittSaksnummer(eq(EXPECTED_SAKSNUMMER), anyBoolean())).thenReturn(Optional.of(FAGSAK));
        when(fagsakTjeneste.hentJournalpost(EXPECTED_JOURNALPOST_ID)).thenReturn(Optional.empty());

        // test
        var saksnummer = opprettSakTjeneste.opprettSak(EXPECTED_YTELSE_TYPE, EXPECTED_AKTØR_ID, EXPECTED_JOURNALPOST_ID);

        verify(fagsakTjeneste, times(2)).hentJournalpost(EXPECTED_JOURNALPOST_ID);
        verify(fagsakTjeneste).lagreJournalPost(new Journalpost(EXPECTED_JOURNALPOST_ID, FAGSAK));
        verifyNoMoreInteractions(fagsakTjeneste);

        assertThat(saksnummer).isNotNull().isEqualTo(EXPECTED_SAKSNUMMER);
    }

    @Test
    void validering_av_sak_kaster_exception() {
        when(fagsakTjeneste.finnFagsakGittSaksnummer(EXPECTED_SAKSNUMMER, false)).thenReturn(Optional.empty());
        var fagsak = opprettSakTjeneste.finnSak(EXPECTED_SAKSNUMMER);

        assertThat(fagsak).isEmpty();
    }

    @Test
    void validering_av_sak_som_finnes_returnerer_sak() {
        var expectedFagsak = Optional.of(FAGSAK);
        when(fagsakTjeneste.finnFagsakGittSaksnummer(EXPECTED_SAKSNUMMER, false)).thenReturn(expectedFagsak);

        var fagsak = opprettSakTjeneste.finnSak(EXPECTED_SAKSNUMMER);

        assertThat(fagsak).isNotEmpty().isEqualTo(expectedFagsak);
    }

    @Test
    void skal_knytte_sak_og_journalpost_knytting_finnes_gjør_ingenting() {
        var journalpostMock = mock(Journalpost.class);
        when(journalpostMock.getFagsak()).thenReturn(FAGSAK);
        when(fagsakTjeneste.hentJournalpost(EXPECTED_JOURNALPOST_ID)).thenReturn(Optional.of(journalpostMock));

        opprettSakTjeneste.knyttSakOgJournalpost(EXPECTED_SAKSNUMMER, EXPECTED_JOURNALPOST_ID);

        verifyNoMoreInteractions(fagsakTjeneste);
    }

    @Test
    void skal_kaste_exception_om_knytting_allerede_finnes() {
        var journalpostMock = mock(Journalpost.class);
        when(journalpostMock.getFagsak()).thenReturn(Fagsak.opprettNy(EXPECTED_YTELSE_TYPE, NavBruker.opprettNyNB(EXPECTED_AKTØR_ID), new Saksnummer("12343432")));
        when(fagsakTjeneste.hentJournalpost(EXPECTED_JOURNALPOST_ID)).thenReturn(Optional.of(journalpostMock));

        assertThrows(TekniskException.class, () -> opprettSakTjeneste.knyttSakOgJournalpost(EXPECTED_SAKSNUMMER, EXPECTED_JOURNALPOST_ID));

        verifyNoMoreInteractions(fagsakTjeneste);
    }
}
