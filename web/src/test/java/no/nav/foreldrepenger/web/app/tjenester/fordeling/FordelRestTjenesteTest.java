package no.nav.foreldrepenger.web.app.tjenester.fordeling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kontrakter.fordel.SaksnummerDto;
import no.nav.foreldrepenger.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakOrchestrator;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.FordelRestTjeneste.AbacSaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.FordelRestTjeneste.AbacVurderFagsystemDto;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class FordelRestTjenesteTest {

    private static final AktørId AKTØR_ID_MOR = AktørId.dummy();

    @Mock
    private SaksbehandlingDokumentmottakTjeneste dokumentmottakTjenesteMock;
    @Mock
    private OpprettSakOrchestrator opprettSakOrchestratorMock;
    @Mock
    private OpprettSakTjeneste opprettSakTjenesteMock;
    @Mock
    private FagsakTjeneste fagsakTjenesteMock;

    @Mock
    private VurderFagsystemFellesTjeneste vurderFagsystemTjenesteMock;
    @Mock
    private FamilieHendelseRepository familieHendelseRepositoryMock;
    @Mock
    private BehandlingRepositoryProvider behandlingRepositoryProviderMock;
    @Mock
    private BehandlingRepository behandlingRepositoryMock;
    @Mock
    private SakInfoDtoTjeneste sakInfoDtoTjenesteMock;

    private FordelRestTjeneste fordelRestTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setup(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var fagsakTjeneste = new FagsakTjeneste(repositoryProvider.getFagsakRepository(), repositoryProvider.getSøknadRepository(), null);
        fordelRestTjeneste = new FordelRestTjeneste(dokumentmottakTjenesteMock, fagsakTjeneste, opprettSakOrchestratorMock, opprettSakTjenesteMock, repositoryProvider, vurderFagsystemTjenesteMock, sakInfoDtoTjenesteMock);
    }

    @Test
    void skalReturnereFagsystemVedtaksløsning() {
        var saksnummer = new Saksnummer("12345");
        var innDto = new AbacVurderFagsystemDto("1234", true, AKTØR_ID_MOR.getId(), "ab0047");
        var behandlendeFagsystem = new BehandlendeFagsystem(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING, saksnummer);

        when(vurderFagsystemTjenesteMock.vurderFagsystem(any(VurderFagsystem.class))).thenReturn(behandlendeFagsystem);

        var result = fordelRestTjeneste.vurderFagsystem(innDto);

        assertThat(result).isNotNull();
        assertThat(result.getSaksnummer()).contains(saksnummer.getVerdi());
        assertThat(result.isBehandlesIVedtaksløsningen()).isTrue();
    }

    @Test
    void skalReturnereFagsystemManuell() {
        var saksnummer = new Saksnummer("TEST1");
        var journalpostId = new JournalpostId("1234");
        var innDto = new AbacVurderFagsystemDto(journalpostId.getVerdi(), false, AKTØR_ID_MOR.getId(), "ab0047");
        innDto.setDokumentTypeIdOffisiellKode(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL.getOffisiellKode());
        var behandlendeFagsystem = new BehandlendeFagsystem(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING, saksnummer);

        when(vurderFagsystemTjenesteMock.vurderFagsystem(any(VurderFagsystem.class))).thenReturn(behandlendeFagsystem);

        var result = fordelRestTjeneste.vurderFagsystem(innDto);

        assertThat(result).isNotNull();
        assertThat(result.isManuellVurdering()).isTrue();
    }

    @Test
    void skalReturnereFagsakinformasjonMedBehandlingTemaOgAktørId() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        scenario.medSaksnummer(new Saksnummer("TEST2")).medSøknadHendelse().medFødselsDato(LocalDate.now());
        scenario.lagre(repositoryProvider);
        var result = fordelRestTjeneste.fagsak(new AbacSaksnummerDto("TEST2"));

        assertThat(result).isNotNull();
        assertThat(new AktørId(result.getAktørId())).isEqualTo(AKTØR_ID_MOR);
        assertThat(result.getBehandlingstemaOffisiellKode()).isEqualTo(BehandlingTema.FORELDREPENGER_FØDSEL.getOffisiellKode());
    }

    @Test
    void skalReturnereNullNårFagsakErStengt() {
        var saknr = new Saksnummer("TEST3");
        var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        scenario.medSaksnummer(saknr).medSøknadHendelse().medFødselsDato(LocalDate.now());
        var behandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRepository().fagsakSkalStengesForBruk(behandling.getFagsakId());
        var result = fordelRestTjeneste.fagsak(new AbacSaksnummerDto("TEST3"));

        assertThat(result).isNull();
    }

    @Test
    void skalReturnereAlleBrukersSaker() {
        var saknr1 = new Saksnummer("TEST3");
        var saknr2 = new Saksnummer("TEST4");
        var foreldrepenger = FagsakYtelseType.FORELDREPENGER;
        var forventetYtelseType = SakInfoDto.FagsakYtelseTypeDto.FORELDREPENGER;
        var forventetStatus = SakInfoDto.FagsakStatusDto.UNDER_BEHANDLING;
        var opprettetTidSak1 = LocalDateTime.now().minusMonths(16);
        var opprettetTidSak2 = LocalDateTime.now();
        var skjæringstidspunkt = LocalDate.now().minusMonths(15);
        var førsteuttaksdato = LocalDate.now().minusMonths(6);


        var fagsak1 = Fagsak.opprettNy(foreldrepenger, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr1);
        fagsak1.setOpprettetTidspunkt(opprettetTidSak1);
        fagsak1.setId(125L);

        var fagsak2 = Fagsak.opprettNy(foreldrepenger, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr2);
        fagsak2.setOpprettetTidspunkt(opprettetTidSak2);
        fagsak2.setEndretTidspunkt(opprettetTidSak2);
        fagsak2.setId(126L);

        when(behandlingRepositoryProviderMock.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);
        when(behandlingRepositoryProviderMock.getFamilieHendelseRepository()).thenReturn(familieHendelseRepositoryMock);

        var sakDto1 = new SakInfoDto(new SaksnummerDto(saknr1.getVerdi()),  forventetYtelseType, opprettetTidSak1.toLocalDate(), forventetStatus, new SakInfoDto.FamiliehendelseInfoDto(skjæringstidspunkt, SakInfoDto.FamilieHendelseTypeDto.FØDSEL), førsteuttaksdato);
        var sakDto2 = new SakInfoDto(new SaksnummerDto(saknr2.getVerdi()), forventetYtelseType, opprettetTidSak2.toLocalDate(), forventetStatus, null, null);

        when(sakInfoDtoTjenesteMock.mapSakInfoDto(fagsak1)).thenReturn(sakDto1);
        when(sakInfoDtoTjenesteMock.mapSakInfoDto(fagsak2)).thenReturn(sakDto2);
        when(fagsakTjenesteMock.finnFagsakerForAktør(any(AktørId.class))).thenReturn(List.of(fagsak1, fagsak2));

        var tjeneste = new FordelRestTjeneste(null, fagsakTjenesteMock, null, null, behandlingRepositoryProviderMock, null, sakInfoDtoTjenesteMock);

        var result = tjeneste.finnAlleSakerForBruker(new FordelRestTjeneste.AktørIdDto(AKTØR_ID_MOR.getId()));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Skal kaste exceptions om aktørId er ikke gyldig.")
    void exception_om_ikke_gyldig_aktørId() {
        var tjeneste = new FordelRestTjeneste(dokumentmottakTjenesteMock, fagsakTjenesteMock, opprettSakOrchestratorMock, opprettSakTjenesteMock, mock(
            BehandlingRepositoryProvider.class), vurderFagsystemTjenesteMock, sakInfoDtoTjenesteMock);

        var aktørIdDto = new FordelRestTjeneste.AktørIdDto("ikke_gyldig_id_haha:)");
        var exception = assertThrows(IllegalArgumentException.class, () -> tjeneste.finnAlleSakerForBruker(aktørIdDto));

        var expectedMessage = "Oppgitt aktørId er ikke en gyldig ident.";
        var actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
