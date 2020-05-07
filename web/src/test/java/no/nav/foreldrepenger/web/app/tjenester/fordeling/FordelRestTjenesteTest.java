package no.nav.foreldrepenger.web.app.tjenester.fordeling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kontrakter.fordel.BehandlendeFagsystemDto;
import no.nav.foreldrepenger.kontrakter.fordel.FagsakInfomasjonDto;
import no.nav.foreldrepenger.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakOrchestrator;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.FordelRestTjeneste.AbacSaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.FordelRestTjeneste.AbacVurderFagsystemDto;

public class FordelRestTjenesteTest {

    private static final AktørId AKTØR_ID_MOR = AktørId.dummy();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private SaksbehandlingDokumentmottakTjeneste dokumentmottakTjenesteMock;
    private FagsakTjeneste fagsakTjenesteMock;
    private OpprettSakOrchestrator opprettSakOrchestratorMock;
    private OpprettSakTjeneste opprettSakTjenesteMock;

    private VurderFagsystemFellesTjeneste vurderFagsystemTjenesteMock;

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private FordelRestTjeneste fordelRestTjeneste;

    @Before
    public void setup() {
        dokumentmottakTjenesteMock = mock(SaksbehandlingDokumentmottakTjeneste.class);
        fagsakTjenesteMock = new FagsakTjeneste(repositoryProvider, null);
        opprettSakOrchestratorMock = mock(OpprettSakOrchestrator.class);
        opprettSakTjenesteMock = mock(OpprettSakTjeneste.class);
        vurderFagsystemTjenesteMock = mock(VurderFagsystemFellesTjeneste.class);

        fordelRestTjeneste = new FordelRestTjeneste(dokumentmottakTjenesteMock,
            fagsakTjenesteMock, opprettSakOrchestratorMock, opprettSakTjenesteMock,repositoryProvider,vurderFagsystemTjenesteMock);
    }

    @Test
    public void skalReturnereFagsystemVedtaksløsning() {
        Saksnummer saksnummer = new Saksnummer("12345");
        AbacVurderFagsystemDto innDto = new AbacVurderFagsystemDto("1234", true, AKTØR_ID_MOR.getId(), "ab0047");
        BehandlendeFagsystem behandlendeFagsystem = new BehandlendeFagsystem(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        behandlendeFagsystem = behandlendeFagsystem.medSaksnummer(saksnummer);

        when(vurderFagsystemTjenesteMock.vurderFagsystem(any(VurderFagsystem.class))).thenReturn(behandlendeFagsystem);

        BehandlendeFagsystemDto result = fordelRestTjeneste.vurderFagsystem(innDto);

        assertThat(result).isNotNull();
        assertThat(String.valueOf(result.getSaksnummer().get())).isEqualTo(saksnummer.getVerdi());
        assertThat(result.isBehandlesIVedtaksløsningen()).isTrue();
    }

    @Test
    public void skalReturnereFagsystemManuell() {
        Saksnummer saksnummer = new Saksnummer("12345");
        JournalpostId journalpostId = new JournalpostId("1234");
        AbacVurderFagsystemDto innDto = new AbacVurderFagsystemDto(journalpostId.getVerdi(), false, AKTØR_ID_MOR.getId(), "ab0047");
        innDto.setDokumentTypeIdOffisiellKode(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL.getOffisiellKode());
        BehandlendeFagsystem behandlendeFagsystem = new BehandlendeFagsystem(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        behandlendeFagsystem = behandlendeFagsystem.medSaksnummer(saksnummer);

        when(vurderFagsystemTjenesteMock.vurderFagsystem(any(VurderFagsystem.class))).thenReturn(behandlendeFagsystem);

        BehandlendeFagsystemDto result = fordelRestTjeneste.vurderFagsystem(innDto);

        assertThat(result).isNotNull();
        assertThat(result.isManuellVurdering()).isTrue();
    }

    @Test
    public void skalReturnereFagsakinformasjonMedBehandlingTemaOgAktørId() {
        final ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        scenario.medSaksnummer(new Saksnummer("1")).medSøknadHendelse().medFødselsDato(LocalDate.now());
        scenario.lagre(repositoryProvider);
        FagsakInfomasjonDto result = fordelRestTjeneste.fagsak(new AbacSaksnummerDto("1"));

        assertThat(result).isNotNull();
        assertThat(new AktørId(result.getAktørId())).isEqualTo(AKTØR_ID_MOR);
        assertThat(result.getBehandlingstemaOffisiellKode()).isEqualTo(BehandlingTema.FORELDREPENGER_FØDSEL.getOffisiellKode());
    }

    @Test
    public void skalReturnereNullNårFagsakErStengt() {
        final Saksnummer saknr = new Saksnummer("1");
        final ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(AKTØR_ID_MOR);
        scenario.medSaksnummer(saknr).medSøknadHendelse().medFødselsDato(LocalDate.now());
        Behandling behandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRepository().fagsakSkalBehandlesAvInfotrygd(behandling.getFagsakId());
        FagsakInfomasjonDto result = fordelRestTjeneste.fagsak(new AbacSaksnummerDto("1"));

        assertThat(result).isNull();
    }

}
