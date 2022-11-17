package no.nav.foreldrepenger.web.app.tjenester.gosys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakOrchestrator;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.gosys.opprettSak.OpprettSakRequest;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.TekniskException;

@ExtendWith(MockitoExtension.class)
class GosysRestTjenesteTest extends EntityManagerAwareTest {
    private static AktørId AKTØR_ID = AktørId.dummy();
    private static String ES_FOD = BehandlingTema.ENGANGSSTØNAD_FØDSEL.getOffisiellKode();
    private static String ES_ADP = BehandlingTema.ENGANGSSTØNAD_ADOPSJON.getOffisiellKode();
    private static String ES_GEN = BehandlingTema.ENGANGSSTØNAD.getOffisiellKode();
    private static String FP_GEN = BehandlingTema.FORELDREPENGER.getOffisiellKode();
    private static String FP_FOD = BehandlingTema.FORELDREPENGER_FØDSEL.getOffisiellKode();

    private GosysRestTjeneste gosysRestTjeneste; // objektet vi tester

    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private OpprettSakTjeneste opprettSakTjeneste;
    @Mock
    private FagsakRepository fagsakRepository;
    @Mock
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    private final String JOURNALPOST = "1234";
    private final JournalpostId JOURNALPOST_ID = new JournalpostId(JOURNALPOST);

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        OpprettSakOrchestrator opprettSakOrchestrator = new OpprettSakOrchestrator(opprettSakTjeneste, fagsakRepository);
        lenient().when(opprettSakTjeneste.utledYtelseType(any(BehandlingTema.class))).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        lenient().when(fagsakRepository.hentForBruker(any())).thenReturn(Collections.emptyList());
        gosysRestTjeneste = new GosysRestTjeneste(repositoryProvider, opprettSakOrchestrator, dokumentArkivTjeneste);
    }

    @Test
    public void skal_konvertere_fagsak_for_engangsstønad_ved_fødsel_til_ekstern_representasjon() {
        final var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSaksnummer(new Saksnummer("1337"));
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        final var behandling = scenario.lagre(repositoryProvider);

        var respons = gosysRestTjeneste.lagFinnSakResponse(Collections.singletonList(behandling.getFagsak()));

        assertThat(respons.sakListe()).hasSize(1);
        var sak = respons.sakListe().get(0);
        assertThat(sak.behandlingstema().value()).isEqualTo("ab0050"); // betyr engangsstønad ved fødsel
        assertThat(sak.behandlingstema().termnavn()).isEqualTo("Engangsstønad ved fødsel");
        assertThat(sak.sakId()).isEqualTo("1337");
    }

    @Test
    public void skal_konvertere_fagsak_for_engangsstønad_ved_adopsjon_til_ekstern_representasjon() {
        final var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSaksnummer(new Saksnummer("1337"));
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));
        final var behandling = scenario.lagre(repositoryProvider);

        var respons = gosysRestTjeneste.lagFinnSakResponse(Collections.singletonList(behandling.getFagsak()));

        assertThat(respons.sakListe()).hasSize(1);
        var sak = respons.sakListe().get(0);
        assertThat(sak.behandlingstema().value()).isEqualTo("ab0027"); // betyr engangsstønad ved adopsjon
        assertThat(sak.behandlingstema().termnavn()).isEqualTo("Engangsstønad ved adopsjon");
        assertThat(sak.sakId()).isEqualTo("1337");
    }

    @Test
    public void skal_konvertere_fagsak_uten_behandlinger_til_ekstern_representasjon() {
        var navBruker = new NavBrukerBuilder()
            .medAktørId(AktørId.dummy())
            .medKjønn(NavBrukerKjønn.KVINNE)
            .build();

        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker, null, new Saksnummer("1338"));
        fagsak.setId(1L);
        var respons = gosysRestTjeneste.lagFinnSakResponse(Collections.singletonList(fagsak));

        assertThat(respons.sakListe()).hasSize(1);
        var sak = respons.sakListe().get(0);
        assertThat(sak.behandlingstema().value()).isEqualTo("ab0326"); // betyr foreldrepenger
        assertThat(sak.behandlingstema().termnavn()).isEqualTo("Foreldrepenger");
        assertThat(sak.sakId()).isEqualTo("1338");
    }

    @Test
    public void skal_konvertere_svangerskapspenger_fagsak_uten_behandlinger_til_ekstern_representasjon() {
        var navBruker = new NavBrukerBuilder()
            .medAktørId(AktørId.dummy())
            .medKjønn(NavBrukerKjønn.KVINNE)
            .build();

        var fagsak = Fagsak.opprettNy(FagsakYtelseType.SVANGERSKAPSPENGER, navBruker, null, new Saksnummer("1339"));
        fagsak.setId(1L);
        var respons = gosysRestTjeneste.lagFinnSakResponse(Collections.singletonList(fagsak));

        assertThat(respons.sakListe()).hasSize(1);
        var sak = respons.sakListe().get(0);
        assertThat(sak.behandlingstema().value()).isEqualTo("ab0126"); // betyr svangerskapspenger
        assertThat(sak.behandlingstema().termnavn()).isEqualTo("Svangerskapspenger");
        assertThat(sak.sakId()).isEqualTo("1339");
    }


    @Test
    public void test_feilhandering_mangler_journaposId() {
        var request = createOpprettSakRequest(null, AKTØR_ID, "ab0050");
        assertThrows(NullPointerException.class, () -> gosysRestTjeneste.opprettSak(request));
    }

    @Test
    public void test_feilhandering_mangler_behandlingstema() {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, null);
        assertThrows(TekniskException.class, () -> gosysRestTjeneste.opprettSak(request));
    }

    @Test
    public void test_feilhandering_ukjent_behandlingstema() {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, "xx1234");
        assertThrows(TekniskException.class, () -> gosysRestTjeneste.opprettSak(request));
    }

    @Test
    public void test_feilhandering_mangler_aktorId() {
        var request = createOpprettSakRequest(JOURNALPOST, null, ES_FOD);
        assertThrows(TekniskException.class, () -> gosysRestTjeneste.opprettSak(request));
    }

    @Test
    public void test_opprettSak_ok_fødsel() throws Exception {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        final Long FAGSAKID = 1L;
        final var expectedSakId = new Saksnummer("02");

        var fagsak = mockFagsak(FAGSAKID, expectedSakId);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL).build()).build()));

        var response = gosysRestTjeneste.opprettSak(request);
        assertThat(response.sakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());
    }

    @Test
    public void test_opprettSak_ok_fødsel_udefinert_doktypesatt() throws Exception {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        final Long FAGSAKID = 1L;
        final var expectedSakId = new Saksnummer("02");

        var fagsak = mockFagsak(FAGSAKID, expectedSakId);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(fagsakRepository.hentJournalpost(any())).thenReturn(Optional.empty());
        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL).build()).build()));

        // Act
        var response = gosysRestTjeneste.opprettSak(request);

        var captor = ArgumentCaptor.forClass(FagsakYtelseType.class);
        verify(opprettSakTjeneste, times(1)).opprettSakVL(any(AktørId.class), captor.capture(), any(JournalpostId.class));
        var bt = captor.getValue();
        assertThat(bt).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);

        assertThat(response.sakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());
    }

    @Test
    public void test_opprettSak_unntak_klagedokument() {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.KLAGE_DOKUMENT).build()).build()));

        assertThrows(FunksjonellException.class, () -> gosysRestTjeneste.opprettSak(request));
    }

    @Test
    public void test_opprettSak_unntak_endring() {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD).build()).build()));

        assertThrows(FunksjonellException.class, () -> gosysRestTjeneste.opprettSak(request));
    }

    @Test
    public void test_opprettSak_unntak_im_annen_ytelse() {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, FP_FOD);

        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.INNTEKTSMELDING).build()).build()));
        when(dokumentArkivTjeneste.hentStrukturertDokument(any(), any())).thenReturn("<IMheader><ytelse>Svangerskapspenger</ytelse>");

        assertThrows(FunksjonellException.class, () -> gosysRestTjeneste.opprettSak(request));
    }

    @Test
    public void test_opprettSak_unntak_im() throws Exception {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, FP_FOD);

        final Long FAGSAKID = 1L;
        final var expectedSakId = new Saksnummer("02");

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

        var response = gosysRestTjeneste.opprettSak(request);
        assertThat(response.sakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());

        lenient().when(fagsak.getStatus()).thenReturn(FagsakStatus.LØPENDE);
        when(fagsak.erÅpen()).thenReturn(true);

        assertThrows(TekniskException.class, () -> gosysRestTjeneste.opprettSak(request));
    }

    @Test
    public void test_opprettSak_ok_adopsjon() throws Exception {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_ADP);

        final Long FAGSAKID = 1L;
        final var expectedSakId = new Saksnummer("02");

        var fagsak = mockFagsak(FAGSAKID, expectedSakId);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON).build()).build()));

        var response = gosysRestTjeneste.opprettSak(request);
        assertThat(response.sakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());
    }

    @Test
    public void test_opprettSak_unntak_klageelleramnnke() {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_FOD);

        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.KLAGE_DOKUMENT).build()).build()));
        assertThrows(FunksjonellException.class, () -> gosysRestTjeneste.opprettSak(request));
    }

    @Test
    public void test_opprettSak_ok_annen_engangsstønad() throws Exception {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, ES_GEN);

        final Long FAGSAKID = 1L;
        final var expectedSakId = new Saksnummer("02");

        var fagsak = mockFagsak(FAGSAKID, expectedSakId);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL).build()).build()));

        var response = gosysRestTjeneste.opprettSak(request);
        assertThat(response.sakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());
    }

    @Test
    public void test_opprettSak_ok_annen_engangsstønad_doktypesatt() throws Exception {
        var request = createOpprettSakRequest(JOURNALPOST, AKTØR_ID, FP_GEN);

        final Long FAGSAKID = 1L;
        final var expectedSakId = new Saksnummer("02");

        var fagsak = mockFagsak(FAGSAKID, expectedSakId);
        when(opprettSakTjeneste.opprettSakVL(AKTØR_ID, FagsakYtelseType.ENGANGSTØNAD, JOURNALPOST_ID)).thenReturn(fagsak);
        when(fagsakRepository.hentJournalpost(any())).thenReturn(Optional.empty());
        when(dokumentArkivTjeneste.hentJournalpostForSak(any())).thenReturn(Optional.of(ArkivJournalPost.Builder.ny()
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId("100").medDokumentTypeId(DokumentTypeId.INNTEKTSMELDING).build()).build()));
        when(dokumentArkivTjeneste.hentStrukturertDokument(any(), any())).thenReturn("<IMheader><ytelse>Foreldrepenger</ytelse>");

        // Act
        var response = gosysRestTjeneste.opprettSak(request);

        var captor = ArgumentCaptor.forClass(FagsakYtelseType.class);
        verify(opprettSakTjeneste, times(1)).opprettSakVL(any(AktørId.class), captor.capture(), any(JournalpostId.class));
        var bt = captor.getValue();
        assertThat(bt).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);

        assertThat(response.sakId()).as("Forventer at saksnummer blir returnert ut fra tjenesten.").isEqualTo(expectedSakId.getVerdi());
    }

    private Fagsak mockFagsak(Long fagsakId, Saksnummer saksnummer) {
        var fagsak = mock(Fagsak.class);
        lenient().when(fagsak.getId()).thenReturn(fagsakId);
        lenient().when(fagsak.getSaksnummer()).thenReturn(saksnummer);
        return fagsak;
    }

    private OpprettSakRequest createOpprettSakRequest(String journalpostId, AktørId aktørId, String behandlingstema) {
        return new OpprettSakRequest(journalpostId, behandlingstema, aktørId == null ? null : aktørId.getId());
    }
}
