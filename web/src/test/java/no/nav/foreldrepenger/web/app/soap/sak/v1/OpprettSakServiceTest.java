package no.nav.foreldrepenger.web.app.soap.sak.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakOrchestrator;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakTjeneste;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.binding.OpprettSakUgyldigInput;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.informasjon.Aktoer;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.informasjon.Behandlingstema;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.meldinger.OpprettSakRequest;
import no.nav.vedtak.exception.FunksjonellException;

@ExtendWith(MockitoExtension.class)
class OpprettSakServiceTest {

    private static AktørId AKTØR_ID = AktørId.dummy();
    private static String ES_FOD = BehandlingTema.ENGANGSSTØNAD_FØDSEL.getOffisiellKode();
    private static String ES_ADP = BehandlingTema.ENGANGSSTØNAD_ADOPSJON.getOffisiellKode();
    private static String ES_GEN = BehandlingTema.ENGANGSSTØNAD.getOffisiellKode();
    private static String FP_GEN = BehandlingTema.FORELDREPENGER.getOffisiellKode();
    private static String FP_FOD = BehandlingTema.FORELDREPENGER_FØDSEL.getOffisiellKode();

    private OpprettSakService service;

    @Mock
    private OpprettSakTjeneste opprettSakTjeneste;
    @Mock
    private FagsakRepository fagsakRepository;
    @Mock
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    private OpprettSakOrchestrator opprettSakOrchestrator;

    private final String JOURNALPOST = "1234";
    private final JournalpostId JOURNALPOST_ID = new JournalpostId(JOURNALPOST);

    @BeforeEach
    public void before() {
        opprettSakOrchestrator = new OpprettSakOrchestrator(opprettSakTjeneste, fagsakRepository);
        service = new OpprettSakService(opprettSakOrchestrator, dokumentArkivTjeneste);
        lenient().when(opprettSakTjeneste.utledYtelseType(any(BehandlingTema.class))).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        lenient().when(fagsakRepository.hentForBruker(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void test_feilhandering_mangler_journaposId() {
        var request = createOpprettSakRequest(null, AKTØR_ID, "ab0050");
        assertThrows(NullPointerException.class, () -> service.opprettSak(request));
    }

    @Test
    void test_feilhandering_mangler_behandlingstema() {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, null);
        assertThrows(OpprettSakUgyldigInput.class, () -> service.opprettSak(request));
    }

    @Test
    void test_feilhandering_ukjent_behandlingstema() {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, "xx1234");
        assertThrows(OpprettSakUgyldigInput.class, () -> service.opprettSak(request));
    }

    @Test
    void test_feilhandering_mangler_aktorId() {
        var request = createOpprettSakRequest(JOURNALPOST, null, ES_FOD);
        assertThrows(OpprettSakUgyldigInput.class, () -> service.opprettSak(request));
    }

    @Test
    void test_opprettSak_ok_fødsel() throws Exception {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        final Long FAGSAKID = 1L;
        var expectedSakId = new Saksnummer("02");

        var fagsak = mockFagsak(FAGSAKID, expectedSakId);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL).build()).build()));

        var response = service.opprettSak(request);
        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());
    }

    @Test
    void test_opprettSak_ok_fødsel_udefinert_doktypesatt() throws Exception {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        final Long FAGSAKID = 1L;
        var expectedSakId = new Saksnummer("02");

        var fagsak = mockFagsak(FAGSAKID, expectedSakId);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(fagsakRepository.hentJournalpost(any())).thenReturn(Optional.empty());
        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL).build()).build()));

        // Act
        var response = service.opprettSak(request);

        var captor = ArgumentCaptor.forClass(FagsakYtelseType.class);
        verify(opprettSakTjeneste, times(1)).opprettSakVL(any(AktørId.class), captor.capture(), any(JournalpostId.class));
        var bt = captor.getValue();
        assertThat(bt).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);

        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());
    }

    @Test
    void test_opprettSak_unntak_klagedokument() {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.KLAGE_DOKUMENT).build()).build()));

        assertThrows(FunksjonellException.class, () -> service.opprettSak(request));
    }

    @Test
    void test_opprettSak_unntak_endring() {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD).build()).build()));

        assertThrows(FunksjonellException.class, () -> service.opprettSak(request));
    }

    @Test
    void test_opprettSak_unntak_im_annen_ytelse() {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, FP_FOD);

        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.INNTEKTSMELDING).build()).build()));
        when(dokumentArkivTjeneste.hentStrukturertDokument(any(), any())).thenReturn("<IMheader><ytelse>Svangerskapspenger</ytelse>");

        assertThrows(FunksjonellException.class, () -> service.opprettSak(request));
    }

    @Test
    void test_opprettSak_unntak_im() throws Exception {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, FP_FOD);

        final Long FAGSAKID = 1L;
        var expectedSakId = new Saksnummer("02");

        var fagsak = mockFagsak(FAGSAKID, expectedSakId);
        when(fagsak.getStatus()).thenReturn(FagsakStatus.AVSLUTTET);
        when(fagsak.erÅpen()).thenReturn(false);
        when(fagsak.getYtelseType()).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(opprettSakTjeneste.utledYtelseType(BehandlingTema.FORELDREPENGER_FØDSEL)).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.FORELDREPENGER, JOURNALPOST_ID)).thenReturn(fagsak);
        when(fagsakRepository.hentForBruker(any())).thenReturn(List.of(fagsak));
        when(fagsakRepository.hentJournalpost(Mockito.any())).thenReturn(Optional.empty());
        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.INNTEKTSMELDING).build()).build()));
        when(dokumentArkivTjeneste.hentStrukturertDokument(any(), any())).thenReturn("<IMheader><ytelse>Foreldrepenger</ytelse>");

        var response = service.opprettSak(request);
        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());

        lenient().when(fagsak.getStatus()).thenReturn(FagsakStatus.LØPENDE);
        when(fagsak.erÅpen()).thenReturn(true);

        assertThrows(OpprettSakUgyldigInput.class, () -> service.opprettSak(request));
    }

    @Test
    void test_opprettSak_ok_adopsjon() throws Exception {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_ADP);

        final Long FAGSAKID = 1L;
        var expectedSakId = new Saksnummer("02");

        var fagsak = mockFagsak(FAGSAKID, expectedSakId);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON).build()).build()));

        var response = service.opprettSak(request);
        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());
    }

    @Test
    void test_opprettSak_unntak_klageelleramnnke() {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.KLAGE_DOKUMENT).build()).build()));
        assertThrows(FunksjonellException.class, () -> service.opprettSak(request));
    }

    @Test
    void test_opprettSak_ok_annen_engangsstønad() throws Exception {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_GEN);

        final Long FAGSAKID = 1L;
        var expectedSakId = new Saksnummer("02");

        var fagsak = mockFagsak(FAGSAKID, expectedSakId);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL).build()).build()));

        var response = service.opprettSak(request);
        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());
    }

    @Test
    void test_opprettSak_ok_annen_engangsstønad_doktypesatt() throws Exception {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, FP_GEN);

        final Long FAGSAKID = 1L;
        var expectedSakId = new Saksnummer("02");

        var fagsak = mockFagsak(FAGSAKID, expectedSakId);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(fagsakRepository.hentJournalpost(any())).thenReturn(Optional.empty());
        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.INNTEKTSMELDING).build()).build()));
        when(dokumentArkivTjeneste.hentStrukturertDokument(any(), any())).thenReturn("<IMheader><ytelse>Foreldrepenger</ytelse>");

        // Act
        var response = service.opprettSak(request);

        var captor = ArgumentCaptor.forClass(FagsakYtelseType.class);
        verify(opprettSakTjeneste, times(1)).opprettSakVL(any(AktørId.class), captor.capture(), any(JournalpostId.class));
        var bt = captor.getValue();
        assertThat(bt).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);

        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());

    }

    private Fagsak mockFagsak(Long fagsakId, Saksnummer saksnummer) {
        var fagsak = mock(Fagsak.class);
        lenient().when(fagsak.getId()).thenReturn(fagsakId);
        lenient().when(fagsak.getSaksnummer()).thenReturn(saksnummer);
        return fagsak;
    }

    private OpprettSakRequest createOpprettSakRequest(String journalpostId, AktørId aktørId, String behandlingstema) {
        var request = new OpprettSakRequest();
        request.setJournalpostId(journalpostId);
        var behTema = new Behandlingstema();
        behTema.setValue(behandlingstema);
        request.setBehandlingstema(behTema);
        var aktoer = new Aktoer();
        aktoer.setAktoerId(aktørId == null ? null : aktørId.getId());
        request.setSakspart(aktoer);
        return request;
    }
}
