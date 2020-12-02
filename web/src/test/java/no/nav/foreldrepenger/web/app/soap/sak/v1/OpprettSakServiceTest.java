package no.nav.foreldrepenger.web.app.soap.sak.v1;

import static java.lang.Long.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kontrakter.fordel.JournalpostVurderingDto;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakFeil;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakOrchestrator;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakTjeneste;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.binding.OpprettSakUgyldigInput;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.informasjon.Aktoer;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.informasjon.Behandlingstema;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.meldinger.OpprettSakRequest;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.meldinger.OpprettSakResponse;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.TekniskException;

@ExtendWith(MockitoExtension.class)
public class OpprettSakServiceTest {

    private static AktørId AKTØR_ID = AktørId.dummy();
    private static String ES_FOD = BehandlingTema.ENGANGSSTØNAD_FØDSEL.getOffisiellKode();
    private static String ES_ADP = BehandlingTema.ENGANGSSTØNAD_ADOPSJON.getOffisiellKode();
    private static String ES_GEN = BehandlingTema.ENGANGSSTØNAD.getOffisiellKode();
    private static String FP_FOD = BehandlingTema.FORELDREPENGER_FØDSEL.getOffisiellKode();

    private OpprettSakService service;

    @Mock
    private OpprettSakTjeneste opprettSakTjeneste;
    @Mock
    private FagsakRepository fagsakRepository;
    @Mock
    private FpfordelRestKlient restKlient;
    private OpprettSakOrchestrator opprettSakOrchestrator;

    private final String JOURNALPOST = "1234";
    private final JournalpostId JOURNALPOST_ID = new JournalpostId(JOURNALPOST);

    @BeforeEach
    public void before() {
        opprettSakOrchestrator = new OpprettSakOrchestrator(opprettSakTjeneste, fagsakRepository);
        service = new OpprettSakService(opprettSakOrchestrator, restKlient, true);
        lenient().when(opprettSakTjeneste.utledYtelseType(any(BehandlingTema.class))).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        lenient().when(fagsakRepository.hentForBruker(any())).thenReturn(Collections.emptyList());
    }

    @Test
    public void test_feilhandering_mangler_journaposId() {
        OpprettSakRequest request = createOpprettSakRequest(null, AKTØR_ID, "ab0050");
        assertThrows(NullPointerException.class, () -> service.opprettSak(request));
    }

    @Test
    public void test_feilhandering_mangler_behandlingstema() {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, null);
        assertThrows(OpprettSakUgyldigInput.class, () -> service.opprettSak(request));
    }

    @Test
    public void test_feilhandering_ukjent_behandlingstema() {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, "xx1234");
        assertThrows(OpprettSakUgyldigInput.class, () -> service.opprettSak(request));
    }

    @Test
    public void test_feilhandering_mangler_aktorId() {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, null, ES_FOD);
        assertThrows(OpprettSakUgyldigInput.class, () -> service.opprettSak(request));
    }

    @Test
    public void test_opprettSak_finner_ikke_bruker() {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);
        AktørId aktorIdLong = new AktørId(valueOf(request.getSakspart().getAktoerId()));

        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(ES_GEN, true, false));
        when(opprettSakTjeneste.opprettSakVL(any(AktørId.class), eq(FagsakYtelseType.ENGANGSTØNAD), eq(JOURNALPOST_ID)))
                .thenThrow(OpprettSakFeil.FACTORY.finnerIkkePersonMedAktørId(aktorIdLong).toException());

        assertThrows(TekniskException.class, () -> service.opprettSak(request));
    }

    @Test
    public void test_opprettSak_ok_fødsel() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        final Long FAGSAKID = 1l;
        final Saksnummer expectedSakId = new Saksnummer("2");

        Fagsak fagsak = mockFagsak(FAGSAKID);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(opprettSakTjeneste.opprettEllerFinnGsak(AKTØR_ID)).thenReturn(expectedSakId);
        mockOppdaterFagsakMedGsakId(fagsak, expectedSakId);
        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(ES_GEN, true, false));

        OpprettSakResponse response = service.opprettSak(request);
        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());
    }

    @Test
    public void test_opprettSak_ok_fødsel_udefinert_doktypesatt() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        final Long FAGSAKID = 1l;
        final Saksnummer expectedSakId = new Saksnummer("2");

        Fagsak fagsak = mockFagsak(FAGSAKID);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(opprettSakTjeneste.opprettEllerFinnGsak(AKTØR_ID)).thenReturn(expectedSakId);
        when(fagsakRepository.hentJournalpost(any())).thenReturn(Optional.empty());
        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(ES_GEN, true, false));
        mockOppdaterFagsakMedGsakId(fagsak, expectedSakId);

        // Act
        OpprettSakResponse response = service.opprettSak(request);

        ArgumentCaptor<FagsakYtelseType> captor = ArgumentCaptor.forClass(FagsakYtelseType.class);
        verify(opprettSakTjeneste, times(1)).opprettSakVL(any(AktørId.class), captor.capture(), any(JournalpostId.class));
        FagsakYtelseType bt = captor.getValue();
        assertThat(bt).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);

        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());
    }

    @Test
    public void test_opprettSak_unntak_klagedokument() {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        final Long FAGSAKID = 1l;

        Fagsak fagsak = mockFagsak(FAGSAKID);
        lenient().when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        lenient().when(fagsakRepository.hentJournalpost(Mockito.any())).thenReturn(Optional.empty());
        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(BehandlingTema.UDEFINERT.getOffisiellKode(), false, false));

        assertThrows(FunksjonellException.class, () -> service.opprettSak(request));
    }

    @Test
    public void test_opprettSak_unntak_endring() {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        final Long FAGSAKID = 1l;

        Fagsak fagsak = mockFagsak(FAGSAKID);
        lenient().when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        lenient().when(fagsakRepository.hentJournalpost(Mockito.any())).thenReturn(Optional.empty());
        lenient().when(restKlient.utledYtelestypeFor(any()))
                .thenReturn(new JournalpostVurderingDto(BehandlingTema.UDEFINERT.getOffisiellKode(), false, false));

        assertThrows(FunksjonellException.class, () -> service.opprettSak(request));
    }

    @Test
    public void test_opprettSak_unntak_im() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, FP_FOD);

        final Long FAGSAKID = 1l;
        final Saksnummer expectedSakId = new Saksnummer("2");

        Fagsak fagsak = mockFagsak(FAGSAKID);
        when(fagsak.getStatus()).thenReturn(FagsakStatus.AVSLUTTET);
        when(fagsak.erÅpen()).thenReturn(false);
        when(fagsak.getYtelseType()).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(opprettSakTjeneste.utledYtelseType(BehandlingTema.FORELDREPENGER_FØDSEL)).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.FORELDREPENGER, JOURNALPOST_ID)).thenReturn(fagsak);
        when(opprettSakTjeneste.opprettEllerFinnGsak(AKTØR_ID)).thenReturn(expectedSakId);
        when(fagsakRepository.hentForBruker(any())).thenReturn(List.of(fagsak));
        when(fagsakRepository.hentJournalpost(Mockito.any())).thenReturn(Optional.empty());
        when(restKlient.utledYtelestypeFor(any()))
                .thenReturn(new JournalpostVurderingDto(BehandlingTema.FORELDREPENGER.getOffisiellKode(), false, true));

        OpprettSakResponse response = service.opprettSak(request);
        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());

        lenient().when(fagsak.getStatus()).thenReturn(FagsakStatus.LØPENDE);
        when(fagsak.erÅpen()).thenReturn(true);

        assertThrows(OpprettSakUgyldigInput.class, () -> service.opprettSak(request));
    }

    @Test
    public void test_opprettSak_ok_adopsjon() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_ADP);

        final Long FAGSAKID = 1l;
        final Saksnummer expectedSakId = new Saksnummer("2");

        Fagsak fagsak = mockFagsak(FAGSAKID);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(opprettSakTjeneste.opprettEllerFinnGsak(AKTØR_ID)).thenReturn(expectedSakId);
        mockOppdaterFagsakMedGsakId(fagsak, expectedSakId);
        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(ES_GEN, true, false));

        OpprettSakResponse response = service.opprettSak(request);
        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());
    }

    @Test
    public void test_opprettSak_unntak_klageelleramnnke() {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        final Long FAGSAKID = 1l;

        Fagsak fagsak = mockFagsak(FAGSAKID);
        lenient().when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        lenient().when(fagsakRepository.hentJournalpost(Mockito.any())).thenReturn(Optional.empty());
        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(BehandlingTema.UDEFINERT.getOffisiellKode(), false, false));
        assertThrows(FunksjonellException.class, () -> service.opprettSak(request));
    }

    @Test
    public void test_opprettSak_ok_annen_engangsstønad() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_GEN);

        final Long FAGSAKID = 1l;
        final Saksnummer expectedSakId = new Saksnummer("2");

        Fagsak fagsak = mockFagsak(FAGSAKID);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(opprettSakTjeneste.opprettEllerFinnGsak(AKTØR_ID)).thenReturn(expectedSakId);
        mockOppdaterFagsakMedGsakId(fagsak, expectedSakId);
        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(ES_GEN, true, false));

        OpprettSakResponse response = service.opprettSak(request);
        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());
    }

    @Test
    public void test_opprettSak_ok_annen_engangsstønad_doktypesatt() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_GEN);

        final Long FAGSAKID = 1l;
        final Saksnummer expectedSakId = new Saksnummer("2");

        Fagsak fagsak = mockFagsak(FAGSAKID);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(opprettSakTjeneste.opprettEllerFinnGsak(AKTØR_ID)).thenReturn(expectedSakId);
        when(fagsakRepository.hentJournalpost(any())).thenReturn(Optional.empty());
        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(ES_GEN, false, true));

        mockOppdaterFagsakMedGsakId(fagsak, expectedSakId);

        // Act
        OpprettSakResponse response = service.opprettSak(request);

        ArgumentCaptor<FagsakYtelseType> captor = ArgumentCaptor.forClass(FagsakYtelseType.class);
        verify(opprettSakTjeneste, times(1)).opprettSakVL(any(AktørId.class), captor.capture(), any(JournalpostId.class));
        FagsakYtelseType bt = captor.getValue();
        assertThat(bt).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);

        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());

    }

    private void mockOppdaterFagsakMedGsakId(Fagsak fagsak, Saksnummer sakId) {
        doAnswer(invocationOnMock -> {
            fagsak.setSaksnummer(sakId);
            // Whitebox.setInternalState(fagsak, "saksnummer", sakId);
            return null;
        })
                .when(opprettSakTjeneste).oppdaterFagsakMedGsakSaksnummer(anyLong(), any(Saksnummer.class));

    }

    private Fagsak mockFagsak(Long fagsakId) {
        Fagsak fagsak = mock(Fagsak.class);
        lenient().when(fagsak.getId()).thenReturn(fagsakId);
        fagsak.setSaksnummer(null);
        // Whitebox.setInternalState(fagsak, "saksnummer", null);
        lenient().when(fagsak.getSaksnummer()).thenReturn(null);
        return fagsak;
    }

    private OpprettSakRequest createOpprettSakRequest(String journalpostId, AktørId aktørId, String behandlingstema) {
        OpprettSakRequest request = new OpprettSakRequest();
        request.setJournalpostId(journalpostId);
        Behandlingstema behTema = new Behandlingstema();
        behTema.setValue(behandlingstema);
        request.setBehandlingstema(behTema);
        Aktoer aktoer = new Aktoer();
        aktoer.setAktoerId(aktørId == null ? null : aktørId.getId());
        request.setSakspart(aktoer);
        return request;
    }
}
