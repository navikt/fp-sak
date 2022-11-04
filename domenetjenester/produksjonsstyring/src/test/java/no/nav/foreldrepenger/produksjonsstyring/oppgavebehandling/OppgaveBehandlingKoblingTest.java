package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;

public class OppgaveBehandlingKoblingTest extends EntityManagerAwareTest {
    private static final Saksnummer SAKSNUMMER = new Saksnummer("12312312");

    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;

    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;

    @BeforeEach
    public void setup() {
        this.entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        oppgaveBehandlingKoblingRepository = new OppgaveBehandlingKoblingRepository(entityManager);
    }

    @Test
    public void skal_lagre_ned_en_oppgave() {
        // Arrange
        var oppgaveIdFraGSAK = "IDFRAGSAK";
        var behandleSøknad = OppgaveÅrsak.BEHANDLE_SAK;

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        final var familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1)
            .medFødselsDato(LocalDate.now());
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var oppgave = new OppgaveBehandlingKobling(behandleSøknad, oppgaveIdFraGSAK, SAKSNUMMER, behandling.getId());
        var id = lagreOppgave(oppgave);

        // Assert
        var oppgaveFraBase = entityManager.find(OppgaveBehandlingKobling.class, id);
        assertThat(oppgaveFraBase.getOppgaveId()).isEqualTo(oppgaveIdFraGSAK);
    }

    private long lagreOppgave(OppgaveBehandlingKobling oppgave) {
        return oppgaveBehandlingKoblingRepository.lagre(oppgave);
    }

    @Test
    public void skal_knytte_en_oppgave_til_en_behandling() {
        // Arrange
        var oppgaveIdFraGSAK = "IDFRAGSAK";
        var behandleSøknad = OppgaveÅrsak.BEHANDLE_SAK;

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        final var familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1)
            .medFødselsDato(LocalDate.now());
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var oppgave = new OppgaveBehandlingKobling(behandleSøknad, oppgaveIdFraGSAK, SAKSNUMMER, behandling.getId());
        lagreOppgave(oppgave);

        // Assert
        var kobling = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        assertThat(OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(OppgaveÅrsak.BEHANDLE_SAK, kobling)).isNotNull();
    }

    @Test
    public void skal_kunne_ferdigstille_en_eksisterende_oppgave() {
        // Arrange
        var oppgaveIdFraGSAK = "IDFRAGSAK";
        var behandleSøknad = OppgaveÅrsak.BEHANDLE_SAK;
        var saksbehandler = "R160223";

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        final var familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1)
            .medFødselsDato(LocalDate.now());
        var behandling = scenario.lagre(repositoryProvider);

        var oppgave = new OppgaveBehandlingKobling(behandleSøknad, oppgaveIdFraGSAK, SAKSNUMMER, behandling.getId());
        Long id = lagreOppgave(oppgave);

        // Act
        var oppgaverFraBase = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        assertThat(oppgaverFraBase).hasSize(1);
        var oppgaveKoblingFraBase = oppgaverFraBase.get(0);
        oppgaveKoblingFraBase.ferdigstillOppgave(saksbehandler);
        lagreOppgave(oppgaveKoblingFraBase);

        var oppgaveHentetFraBasen = entityManager.find(OppgaveBehandlingKobling.class, oppgaveKoblingFraBase.getId());
        assertThat(oppgaveHentetFraBasen.isFerdigstilt()).isTrue();
    }

}
