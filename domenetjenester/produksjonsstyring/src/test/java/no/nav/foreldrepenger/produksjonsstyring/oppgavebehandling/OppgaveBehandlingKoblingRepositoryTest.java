package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;

public class OppgaveBehandlingKoblingRepositoryTest extends EntityManagerAwareTest {

    private static final Saksnummer DUMMY_SAKSNUMMER = new Saksnummer("1234123");
    private EntityManager entityManager;

    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        oppgaveBehandlingKoblingRepository = new OppgaveBehandlingKoblingRepository(entityManager);
        this.entityManager = entityManager;
    }

    @Test
    public void skal_hente_opp_oppgave_behandling_kobling_basert_på_oppgave_id() {
        // Arrange
        var oppgaveId = "G1502453";
        var behandling = new BasicBehandlingBuilder(getEntityManager()).opprettOgLagreFørstegangssøknad(
            FagsakYtelseType.ENGANGSTØNAD);
        lagOppgave(
            new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, oppgaveId, DUMMY_SAKSNUMMER, behandling.getId()));

        // Act
        var behandlingKoblingOpt = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(
            oppgaveId);

        // Assert
        assertThat(behandlingKoblingOpt).hasValueSatisfying(
            behandlingKobling -> assertThat(behandlingKobling.getOppgaveÅrsak()).isEqualTo(OppgaveÅrsak.BEHANDLE_SAK));

        // Act
        var oppBehandlingKoblingOpt = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(
            behandling.getId(), oppgaveId);

        // Assert
        assertThat(oppBehandlingKoblingOpt).hasValueSatisfying(
            behandlingKobling -> assertThat(behandlingKobling.getOppgaveÅrsak()).isEqualTo(OppgaveÅrsak.BEHANDLE_SAK));
    }

    @Test
    public void skal_hente_opp_oppgave_behandling_kobling_basert_på_saksnummer() {
        // Arrange
        var oppgaveId = "G1502453";
        var behandling = new BasicBehandlingBuilder(getEntityManager()).opprettOgLagreFørstegangssøknad(
            FagsakYtelseType.ENGANGSTØNAD);
        lagOppgave(
            new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, oppgaveId, DUMMY_SAKSNUMMER, behandling.getId()));

        // Act
        var behandlingKoblingOpt = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(
            oppgaveId, DUMMY_SAKSNUMMER);

        // Assert
        assertThat(behandlingKoblingOpt).hasValueSatisfying(
            behandlingKobling -> assertThat(behandlingKobling.getOppgaveÅrsak()).isEqualTo(OppgaveÅrsak.BEHANDLE_SAK));
    }

    @Test
    public void skal_hente_opp_oppgave_behandling_koblinger_for_åpne_oppgaver() {
        // Arrange
        var behandlingAvsl = new BasicBehandlingBuilder(getEntityManager()).opprettOgLagreFørstegangssøknad(FagsakYtelseType.ENGANGSTØNAD);
        var oppgaveAvsl = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, "O1234", DUMMY_SAKSNUMMER, behandlingAvsl.getId());
        oppgaveAvsl.ferdigstillOppgave("I11111");
        var behandlingBehandleAapen = new BasicBehandlingBuilder(getEntityManager()).opprettOgLagreFørstegangssøknad(FagsakYtelseType.ENGANGSTØNAD);
        var oppgaveBehandleAapen = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, "O1235", DUMMY_SAKSNUMMER, behandlingBehandleAapen.getId());
        var behandlingGodkjenn = new BasicBehandlingBuilder(getEntityManager()).opprettOgLagreFørstegangssøknad(FagsakYtelseType.ENGANGSTØNAD);
        var oppgaveGodkjenn = new OppgaveBehandlingKobling(OppgaveÅrsak.GODKJENNE_VEDTAK, "O1236", DUMMY_SAKSNUMMER, behandlingGodkjenn.getId());
        var behandlingRegistrer = new BasicBehandlingBuilder(getEntityManager()).opprettOgLagreFørstegangssøknad(FagsakYtelseType.ENGANGSTØNAD);
        var oppgaveRegistrer = new OppgaveBehandlingKobling(OppgaveÅrsak.REGISTRER_SØKNAD, "O1238", DUMMY_SAKSNUMMER, behandlingRegistrer.getId());
        var behandlingRevurder = new BasicBehandlingBuilder(getEntityManager()).opprettOgLagreFørstegangssøknad(FagsakYtelseType.ENGANGSTØNAD);
        var oppgaveRevurder = new OppgaveBehandlingKobling(OppgaveÅrsak.REVURDER, "O1237", DUMMY_SAKSNUMMER, behandlingRevurder.getId());

        lagOppgave(oppgaveBehandleAapen);
        lagOppgave(oppgaveAvsl);
        lagOppgave(oppgaveGodkjenn);
        lagOppgave(oppgaveRevurder);
        lagOppgave(oppgaveRegistrer);

        // Act
        var behandlingKobling = oppgaveBehandlingKoblingRepository.hentBehandlingerMedUferdigeOppgaverOpprettetTidsrom(LocalDate.now(),
            LocalDate.now(), Set.of(OppgaveÅrsak.BEHANDLE_SAK, OppgaveÅrsak.REVURDER));

        // Assert
        assertThat(behandlingKobling).contains(behandlingBehandleAapen, behandlingRevurder);
        assertThat(behandlingKobling).doesNotContain(behandlingAvsl, behandlingGodkjenn, behandlingRegistrer);

        // Change + reassert
        oppgaveRevurder.ferdigstillOppgave("I11111");
        lagOppgave(oppgaveRevurder);
        behandlingKobling = oppgaveBehandlingKoblingRepository.hentBehandlingerMedUferdigeOppgaverOpprettetTidsrom(LocalDate.now(),
            LocalDate.now(), Set.of(OppgaveÅrsak.BEHANDLE_SAK, OppgaveÅrsak.REVURDER));
        assertThat(behandlingKobling).contains(behandlingBehandleAapen);
        assertThat(behandlingKobling).doesNotContain(behandlingAvsl, behandlingGodkjenn, behandlingRegistrer, behandlingRevurder);

    }

    private void lagOppgave(OppgaveBehandlingKobling oppgaveBehandlingKobling) {
        oppgaveBehandlingKoblingRepository.lagre(oppgaveBehandlingKobling);
        entityManager.flush();
    }


}
