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
import java.util.Optional;

import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakOrchestrator;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.FordelRestTjeneste.AbacSaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.FordelRestTjeneste.AbacVurderFagsystemDto;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
public class FordelRestTjenesteTest {

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
    private FamilieHendelseGrunnlagEntitet familieHendelseGrunnlagEntitetMock;
    @Mock
    private BehandlingRepository behandlingRepositoryMock;
    @Mock
    private FamilieHendelseEntitet familieHendelseEntitetMock;
    private FordelRestTjeneste fordelRestTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setup(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        FagsakTjeneste fagsakTjeneste = new FagsakTjeneste(repositoryProvider.getFagsakRepository(), repositoryProvider.getSøknadRepository(), null);
        fordelRestTjeneste = new FordelRestTjeneste(dokumentmottakTjenesteMock, fagsakTjeneste, opprettSakOrchestratorMock, opprettSakTjenesteMock, repositoryProvider, vurderFagsystemTjenesteMock);
    }

    @Test
    void skalReturnereFagsystemVedtaksløsning() {
        var saksnummer = new Saksnummer("12345");
        var innDto = new AbacVurderFagsystemDto("1234", true, AKTØR_ID_MOR.getId(), "ab0047");
        var behandlendeFagsystem = new BehandlendeFagsystem(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING, saksnummer);

        when(vurderFagsystemTjenesteMock.vurderFagsystem(any(VurderFagsystem.class))).thenReturn(behandlendeFagsystem);

        var result = fordelRestTjeneste.vurderFagsystem(innDto);

        assertThat(result).isNotNull();
        assertThat(result.getSaksnummer().get()).isEqualTo(saksnummer.getVerdi());
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
        final var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        scenario.medSaksnummer(new Saksnummer("TEST2")).medSøknadHendelse().medFødselsDato(LocalDate.now());
        scenario.lagre(repositoryProvider);
        var result = fordelRestTjeneste.fagsak(new AbacSaksnummerDto("TEST2"));

        assertThat(result).isNotNull();
        assertThat(new AktørId(result.getAktørId())).isEqualTo(AKTØR_ID_MOR);
        assertThat(result.getBehandlingstemaOffisiellKode()).isEqualTo(BehandlingTema.FORELDREPENGER_FØDSEL.getOffisiellKode());
    }

    @Test
    void skalReturnereNullNårFagsakErStengt() {
        final var saknr = new Saksnummer("TEST3");
        final var scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
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
        var opprettetTidSak1 = LocalDateTime.now().minusMonths(16);
        var opprettetTidSak2 = LocalDateTime.now();
        var skjæringstidspunkt = LocalDate.now().minusMonths(15);


        Fagsak fagsak1 = Fagsak.opprettNy(foreldrepenger, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr1);
        fagsak1.setOpprettetTidspunkt(opprettetTidSak1);
        fagsak1.setEndretTidspunkt(opprettetTidSak1);
        fagsak1.setId(125L);
        var behandling1 = Behandling.forFørstegangssøknad(fagsak1).build();

        Fagsak fagsak2 = Fagsak.opprettNy(foreldrepenger, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr2);
        fagsak2.setOpprettetTidspunkt(opprettetTidSak2);
        fagsak2.setEndretTidspunkt(opprettetTidSak2);
        fagsak2.setId(126L);

        when(behandlingRepositoryProviderMock.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);
        when(behandlingRepositoryProviderMock.getFamilieHendelseRepository()).thenReturn(familieHendelseRepositoryMock);
        //Sak 1 - har familienhendelse dato
        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak1.getId())).thenReturn(Optional.of(behandling1));
        when(familieHendelseRepositoryMock.hentAggregatHvisEksisterer(behandling1.getId())).thenReturn(Optional.of(familieHendelseGrunnlagEntitetMock));
        when(familieHendelseGrunnlagEntitetMock.getGjeldendeVersjon()).thenReturn(familieHendelseEntitetMock);
        when(familieHendelseEntitetMock.getSkjæringstidspunkt()).thenReturn(skjæringstidspunkt);
        //Sak 2 - har ingen familiehendelse dato
        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak2.getId())).thenReturn(Optional.empty());

        when(fagsakTjenesteMock.finnFagsakerForAktør(any(AktørId.class))).thenReturn(List.of(fagsak1, fagsak2));

        var tjeneste = new FordelRestTjeneste(null, fagsakTjenesteMock, null, null, behandlingRepositoryProviderMock, null);

        var result = tjeneste.finnAlleSakerForBruker(new FordelRestTjeneste.AktørIdDto(AKTØR_ID_MOR.getId()));

        assertThat(result).hasSize(2);

        var sakInfoDto1 = result.get(0);
        assertThat(sakInfoDto1.saksnummer().getSaksnummer()).isEqualTo(saknr1.getVerdi());
        assertThat(sakInfoDto1.status()).isEqualTo(FagsakStatus.OPPRETTET);
        assertThat(sakInfoDto1.opprettetDato()).isEqualTo(opprettetTidSak1.toLocalDate());
        assertThat(sakInfoDto1.endretDato()).isEqualTo(opprettetTidSak1.toLocalDate());
        assertThat(sakInfoDto1.ytelseType()).isEqualTo(foreldrepenger);
        assertThat(sakInfoDto1.gjeldendeFamiliehendelseDato()).isEqualTo(skjæringstidspunkt);

        var sakInfoDto2 = result.get(1);
        assertThat(sakInfoDto2.saksnummer().getSaksnummer()).isEqualTo(saknr2.getVerdi());
        assertThat(sakInfoDto2.status()).isEqualTo(FagsakStatus.OPPRETTET);
        assertThat(sakInfoDto2.opprettetDato()).isEqualTo(opprettetTidSak2.toLocalDate());
        assertThat(sakInfoDto2.endretDato()).isEqualTo(opprettetTidSak2.toLocalDate());
        assertThat(sakInfoDto2.ytelseType()).isEqualTo(foreldrepenger);
        assertThat(sakInfoDto2.gjeldendeFamiliehendelseDato()).isNull();
    }

    @Test
    @DisplayName("Skal kaste exceptions om aktørId er ikke gyldig.")
    void exception_om_ikke_gyldig_aktørId() {
        var tjeneste = new FordelRestTjeneste(dokumentmottakTjenesteMock, fagsakTjenesteMock, opprettSakOrchestratorMock, opprettSakTjenesteMock, mock(
            BehandlingRepositoryProvider.class), vurderFagsystemTjenesteMock);

        var aktørIdDto = new FordelRestTjeneste.AktørIdDto("ikke_gyldig_id_haha:)");
        var exception = assertThrows(IllegalArgumentException.class, () -> tjeneste.finnAlleSakerForBruker(aktørIdDto));

        String expectedMessage = "Oppgitt aktørId er ikke en gyldig ident.";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
