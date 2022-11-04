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
        var behandling = new BasicBehandlingBuilder(getEntityManager()).opprettOgLagreFørstegangssøknad(
            FagsakYtelseType.ENGANGSTØNAD);
        var bsAvsl = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, "O1234",
            DUMMY_SAKSNUMMER, behandling.getId());
        bsAvsl.ferdigstillOppgave("I11111");
        var bsAapen = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, "O1235",
            DUMMY_SAKSNUMMER, behandling.getId());
        var godkjenn = new OppgaveBehandlingKobling(OppgaveÅrsak.GODKJENNE_VEDTAK, "O1236",
            DUMMY_SAKSNUMMER, behandling.getId());
        var registrer = new OppgaveBehandlingKobling(OppgaveÅrsak.REGISTRER_SØKNAD, "O1238",
            DUMMY_SAKSNUMMER, behandling.getId());
        var revurder = new OppgaveBehandlingKobling(OppgaveÅrsak.REVURDER, "O1237",
            DUMMY_SAKSNUMMER, behandling.getId());

        lagOppgave(bsAapen);
        lagOppgave(bsAvsl);
        lagOppgave(godkjenn);
        lagOppgave(revurder);
        lagOppgave(registrer);

        // Act
        var behandlingKobling = oppgaveBehandlingKoblingRepository.hentUferdigeOppgaverOpprettetTidsrom(LocalDate.now(),
            LocalDate.now(), Set.of(OppgaveÅrsak.BEHANDLE_SAK, OppgaveÅrsak.REVURDER));

        // Assert
        assertThat(behandlingKobling).contains(bsAapen, revurder);
        assertThat(behandlingKobling).doesNotContain(godkjenn, registrer, bsAvsl);

        // Change + reassert
        revurder.ferdigstillOppgave("I11111");
        lagOppgave(revurder);
        behandlingKobling = oppgaveBehandlingKoblingRepository.hentUferdigeOppgaverOpprettetTidsrom(LocalDate.now(),
            LocalDate.now(), Set.of(OppgaveÅrsak.BEHANDLE_SAK, OppgaveÅrsak.REVURDER));
        assertThat(behandlingKobling).contains(bsAapen);
        assertThat(behandlingKobling).doesNotContain(godkjenn, registrer, bsAvsl, revurder);

    }

    private void lagOppgave(OppgaveBehandlingKobling oppgaveBehandlingKobling) {
        oppgaveBehandlingKoblingRepository.lagre(oppgaveBehandlingKobling);
        entityManager.flush();
    }


}
