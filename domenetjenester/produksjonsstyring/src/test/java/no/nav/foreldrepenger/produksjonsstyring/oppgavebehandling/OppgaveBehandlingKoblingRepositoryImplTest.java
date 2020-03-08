package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class OppgaveBehandlingKoblingRepositoryImplTest {

    private static final Saksnummer DUMMY_SAKSNUMMER = new Saksnummer("123");
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private Repository repository = repoRule.getRepository();

    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository = new OppgaveBehandlingKoblingRepository(
        repoRule.getEntityManager());

    @Test
    public void skal_hente_opp_oppgave_behandling_kobling_basert_på_oppgave_id() {
        // Arrange
        String oppgaveId = "G1502453";
        Behandling behandling = new BasicBehandlingBuilder(repoRule.getEntityManager()).opprettOgLagreFørstegangssøknad(FagsakYtelseType.ENGANGSTØNAD);
        lagOppgave(new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, oppgaveId, DUMMY_SAKSNUMMER, behandling));

        // Act
        Optional<OppgaveBehandlingKobling> behandlingKoblingOpt = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(oppgaveId);

        // Assert
        assertThat(behandlingKoblingOpt).hasValueSatisfying(behandlingKobling ->
            assertThat(behandlingKobling.getOppgaveÅrsak()).isEqualTo(OppgaveÅrsak.BEHANDLE_SAK)
        );
    }

    @Test
    public void skal_hente_opp_oppgave_behandling_koblinger_for_åpne_oppgaver() {
        // Arrange
        Behandling behandling = new BasicBehandlingBuilder(repoRule.getEntityManager()).opprettOgLagreFørstegangssøknad(FagsakYtelseType.ENGANGSTØNAD);
        OppgaveBehandlingKobling bsAvsl = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, "O1234", DUMMY_SAKSNUMMER, behandling);
        bsAvsl.ferdigstillOppgave("I11111");
        OppgaveBehandlingKobling bsAapen = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, "O1235", DUMMY_SAKSNUMMER, behandling);
        OppgaveBehandlingKobling godkjenn = new OppgaveBehandlingKobling(OppgaveÅrsak.GODKJENNE_VEDTAK, "O1236", DUMMY_SAKSNUMMER, behandling);
        OppgaveBehandlingKobling registrer = new OppgaveBehandlingKobling(OppgaveÅrsak.REGISTRER_SØKNAD, "O1238", DUMMY_SAKSNUMMER, behandling);
        OppgaveBehandlingKobling revurder = new OppgaveBehandlingKobling(OppgaveÅrsak.REVURDER, "O1237", DUMMY_SAKSNUMMER, behandling);

        lagOppgave(bsAapen);
        lagOppgave(bsAvsl);
        lagOppgave(godkjenn);
        lagOppgave(revurder);
        lagOppgave(registrer);

        // Act
        List<OppgaveBehandlingKobling> behandlingKobling = oppgaveBehandlingKoblingRepository.hentUferdigeOppgaverOpprettetTidsrom(LocalDate.now(), LocalDate.now(), Set.of(OppgaveÅrsak.BEHANDLE_SAK, OppgaveÅrsak.REVURDER));

        // Assert
        assertThat(behandlingKobling).hasSize(2);

        // Change + reassert
        revurder.ferdigstillOppgave("I11111");
        lagOppgave(revurder);
        behandlingKobling = oppgaveBehandlingKoblingRepository.hentUferdigeOppgaverOpprettetTidsrom(LocalDate.now(), LocalDate.now(), Set.of(OppgaveÅrsak.BEHANDLE_SAK, OppgaveÅrsak.REVURDER));
        assertThat(behandlingKobling).hasSize(1);

    }

    private void lagOppgave(OppgaveBehandlingKobling oppgaveBehandlingKobling) {
        oppgaveBehandlingKoblingRepository.lagre(oppgaveBehandlingKobling);
        repository.flush();
    }


}
