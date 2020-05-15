package no.nav.foreldrepenger.web.app.soap.sak.v1;

import static java.lang.Long.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
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
import no.nav.vedtak.felles.testutilities.Whitebox;

public class OpprettSakServiceTest {

    private static AktørId AKTØR_ID = AktørId.dummy();
    private static String ES_FOD = BehandlingTema.ENGANGSSTØNAD_FØDSEL.getOffisiellKode();
    private static String ES_ADP = BehandlingTema.ENGANGSSTØNAD_ADOPSJON.getOffisiellKode();
    private static String ES_GEN = BehandlingTema.ENGANGSSTØNAD.getOffisiellKode();
    private static String FP_FOD = BehandlingTema.FORELDREPENGER_FØDSEL.getOffisiellKode();

    private OpprettSakService service;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private OpprettSakTjeneste opprettSakTjeneste;
    @Mock
    private FagsakRepository fagsakRepository;
    @Mock
    private FpfordelRestKlient restKlient;
    private OpprettSakOrchestrator opprettSakOrchestrator;

    private final String JOURNALPOST = "1234";
    private final JournalpostId JOURNALPOST_ID = new JournalpostId(JOURNALPOST);
    private Personinfo personinfo;

    @Before
    public void before() {
        opprettSakTjeneste = mock(OpprettSakTjeneste.class);
        restKlient = mock(FpfordelRestKlient.class);
        fagsakRepository = mock(FagsakRepository.class);
        opprettSakOrchestrator = new OpprettSakOrchestrator(opprettSakTjeneste, fagsakRepository);
        service = new OpprettSakService(opprettSakOrchestrator, restKlient, true);
        personinfo = new Personinfo.Builder().medAktørId(AKTØR_ID).medNavBrukerKjønn(NavBrukerKjønn.KVINNE).medNavn("Lorem Ipsum")
            .medPersonIdent(new PersonIdent(new FiktiveFnr().nesteKvinneFnr())).medFødselsdato(LocalDate.now().minusYears(20)).build();
        when(opprettSakTjeneste.utledYtelseType(any(BehandlingTema.class))).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        when(opprettSakTjeneste.hentBruker(any())).thenReturn(personinfo);
        when(fagsakRepository.hentForBruker(any())).thenReturn(Collections.emptyList());
    }

    @Test(expected = NullPointerException.class)
    public void test_feilhandering_mangler_journaposId() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(null, AKTØR_ID, "ab0050");
        service.opprettSak(request);
    }

    @Test(expected = OpprettSakUgyldigInput.class)
    public void test_feilhandering_mangler_behandlingstema() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, null);
        service.opprettSak(request);
    }

    @Test(expected = OpprettSakUgyldigInput.class)
    public void test_feilhandering_ukjent_behandlingstema() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, "xx1234");
        service.opprettSak(request);
    }

    @Test(expected = OpprettSakUgyldigInput.class)
    public void test_feilhandering_mangler_aktorId() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, null, ES_FOD);
        service.opprettSak(request);
    }

    @Test(expected = TekniskException.class)
    public void test_opprettSak_finner_ikke_bruker() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);
        AktørId aktorIdLong = new AktørId(valueOf(request.getSakspart().getAktoerId()));

        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(ES_GEN, true, false));
        when(opprettSakTjeneste.hentBruker(any(AktørId.class))).thenThrow(OpprettSakFeil.FACTORY.finnerIkkePersonMedAktørId(aktorIdLong).toException());

        service.opprettSak(request);
    }

    @Test
    public void test_opprettSak_ok_fødsel() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        final Long FAGSAKID = 1l;
        final Saksnummer expectedSakId = new Saksnummer("2");

        Fagsak fagsak = mockFagsak(FAGSAKID);
        when(opprettSakTjeneste.opprettSakVL(personinfo, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(opprettSakTjeneste.opprettEllerFinnGsak(fagsak.getId(), personinfo)).thenReturn(expectedSakId);
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
        when(opprettSakTjeneste.opprettSakVL(personinfo, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(opprettSakTjeneste.opprettEllerFinnGsak(fagsak.getId(), personinfo)).thenReturn(expectedSakId);
        when(fagsakRepository.hentJournalpost(any())).thenReturn(Optional.empty());
        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(ES_GEN, true, false));
        mockOppdaterFagsakMedGsakId(fagsak, expectedSakId);

        // Act
        OpprettSakResponse response = service.opprettSak(request);

        ArgumentCaptor<FagsakYtelseType> captor = ArgumentCaptor.forClass(FagsakYtelseType.class);
        verify(opprettSakTjeneste, times(1)).opprettSakVL(any(Personinfo.class), captor.capture(), any(JournalpostId.class));
        FagsakYtelseType bt = captor.getValue();
        assertThat(bt).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);

        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());
    }

    @Test(expected = FunksjonellException.class)
    public void test_opprettSak_unntak_klagedokument() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        final Long FAGSAKID = 1l;
        final Saksnummer expectedSakId = new Saksnummer("2");

        Fagsak fagsak = mockFagsak(FAGSAKID);
        when(opprettSakTjeneste.opprettSakVL(personinfo, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(fagsakRepository.hentJournalpost(Mockito.any())).thenReturn(Optional.empty());
        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(BehandlingTema.UDEFINERT.getOffisiellKode(), false, false));


        @SuppressWarnings("unused")
        OpprettSakResponse response = service.opprettSak(request);
    }

    @Test(expected = FunksjonellException.class)
    public void test_opprettSak_unntak_endring() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        final Long FAGSAKID = 1l;
        final Saksnummer expectedSakId = new Saksnummer("2");

        Fagsak fagsak = mockFagsak(FAGSAKID);
        when(opprettSakTjeneste.opprettSakVL(personinfo, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(fagsakRepository.hentJournalpost(Mockito.any())).thenReturn(Optional.empty());
        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(BehandlingTema.UDEFINERT.getOffisiellKode(), false, false));


        @SuppressWarnings("unused")
        OpprettSakResponse response = service.opprettSak(request);
    }

    @Test(expected = OpprettSakUgyldigInput.class)
    public void test_opprettSak_unntak_im() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, FP_FOD);

        final Long FAGSAKID = 1l;
        final Saksnummer expectedSakId = new Saksnummer("2");

        Fagsak fagsak = mockFagsak(FAGSAKID);
        when(fagsak.getStatus()).thenReturn(FagsakStatus.AVSLUTTET);
        when(fagsak.erÅpen()).thenReturn(false);
        when(fagsak.getYtelseType()).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(opprettSakTjeneste.utledYtelseType(BehandlingTema.FORELDREPENGER_FØDSEL)).thenReturn(FagsakYtelseType.FORELDREPENGER);
        when(opprettSakTjeneste.opprettSakVL(personinfo, FagsakYtelseType.FORELDREPENGER, JOURNALPOST_ID)).thenReturn(fagsak);
        when(opprettSakTjeneste.opprettEllerFinnGsak(fagsak.getId(), personinfo)).thenReturn(expectedSakId);
        when(fagsakRepository.hentForBruker(any())).thenReturn(List.of(fagsak));
        when(fagsakRepository.hentJournalpost(Mockito.any())).thenReturn(Optional.empty());
        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(BehandlingTema.FORELDREPENGER.getOffisiellKode(), false, true));


        OpprettSakResponse response = service.opprettSak(request);
        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());

        when(fagsak.getStatus()).thenReturn(FagsakStatus.LØPENDE);
        when(fagsak.erÅpen()).thenReturn(true);

        @SuppressWarnings("unused")
        OpprettSakResponse response2 = service.opprettSak(request);
    }


    @Test
    public void test_opprettSak_ok_adopsjon() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_ADP);

        final Long FAGSAKID = 1l;
        final Saksnummer expectedSakId = new Saksnummer("2");

        Fagsak fagsak = mockFagsak(FAGSAKID);
        when(opprettSakTjeneste.opprettSakVL(personinfo, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(opprettSakTjeneste.opprettEllerFinnGsak(fagsak.getId(), personinfo)).thenReturn(expectedSakId);
        mockOppdaterFagsakMedGsakId(fagsak, expectedSakId);
        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(ES_GEN, true, false));

        OpprettSakResponse response = service.opprettSak(request);
        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());
    }

    @Test(expected = FunksjonellException.class)
    public void test_opprettSak_unntak_klageelleramnnke() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        final Long FAGSAKID = 1l;
        final Saksnummer expectedSakId = new Saksnummer("2");

        Fagsak fagsak = mockFagsak(FAGSAKID);
        when(opprettSakTjeneste.opprettSakVL(personinfo, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(fagsakRepository.hentJournalpost(Mockito.any())).thenReturn(Optional.empty());
        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(BehandlingTema.UDEFINERT.getOffisiellKode(), false, false));

        @SuppressWarnings("unused")
        OpprettSakResponse response = service.opprettSak(request);
    }

    @Test
    public void test_opprettSak_ok_annen_engangsstønad() throws Exception {
        OpprettSakRequest request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_GEN);

        final Long FAGSAKID = 1l;
        final Saksnummer expectedSakId = new Saksnummer("2");

        Fagsak fagsak = mockFagsak(FAGSAKID);
        when(opprettSakTjeneste.opprettSakVL(personinfo, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(opprettSakTjeneste.opprettEllerFinnGsak(fagsak.getId(), personinfo)).thenReturn(expectedSakId);
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
        when(opprettSakTjeneste.opprettSakVL(personinfo, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(opprettSakTjeneste.opprettEllerFinnGsak(fagsak.getId(), personinfo)).thenReturn(expectedSakId);
        when(fagsakRepository.hentJournalpost(any())).thenReturn(Optional.empty());
        when(restKlient.utledYtelestypeFor(any())).thenReturn(new JournalpostVurderingDto(ES_GEN, false, true));

        mockOppdaterFagsakMedGsakId(fagsak, expectedSakId);

        // Act
        OpprettSakResponse response = service.opprettSak(request);

        ArgumentCaptor<FagsakYtelseType> captor = ArgumentCaptor.forClass(FagsakYtelseType.class);
        verify(opprettSakTjeneste, times(1)).opprettSakVL(any(Personinfo.class), captor.capture(), any(JournalpostId.class));
        FagsakYtelseType bt = captor.getValue();
        assertThat(bt).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);

        assertThat(response.getSakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());

    }

    private void mockOppdaterFagsakMedGsakId(Fagsak fagsak, Saksnummer sakId) {
        doAnswer(invocationOnMock -> { Whitebox.setInternalState(fagsak, "saksnummer", sakId); return null; })
            .when(opprettSakTjeneste).oppdaterFagsakMedGsakSaksnummer(anyLong(), any(Saksnummer.class));

    }


    private Fagsak mockFagsak(Long fagsakId) {
        Fagsak fagsak = mock(Fagsak.class);
        when(fagsak.getId()).thenReturn(fagsakId);
        Whitebox.setInternalState(fagsak, "saksnummer", null);
        when(fagsak.getSaksnummer()).thenReturn((Saksnummer) Whitebox.getInternalState(fagsak, "saksnummer"));
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
