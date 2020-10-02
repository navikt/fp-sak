package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class OppgaveBehandlingKoblingTest {
    private static final Saksnummer SAKSNUMMER = new Saksnummer("123");
    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private Repository repository = repoRule.getRepository();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository = new OppgaveBehandlingKoblingRepository(repoRule.getEntityManager());

    private Fagsak fagsak = FagsakBuilder.nyEngangstønadForMor().build();

    @Before
    public void setup() {
        repository.lagre(fagsak.getNavBruker());
        repository.lagre(fagsak);
        repository.flush();
    }

    @Test
    public void skal_lagre_ned_en_oppgave() {
        // Arrange
        String oppgaveIdFraGSAK = "IDFRAGSAK";
        OppgaveÅrsak behandleSøknad = OppgaveÅrsak.BEHANDLE_SAK;

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        final FamilieHendelseBuilder familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1)
            .medFødselsDato(LocalDate.now());
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        OppgaveBehandlingKobling oppgave = new OppgaveBehandlingKobling(behandleSøknad, oppgaveIdFraGSAK, SAKSNUMMER, behandling.getId());
        long id = lagreOppgave(oppgave);

        // Assert
        OppgaveBehandlingKobling oppgaveFraBase = repository.hent(OppgaveBehandlingKobling.class, id);
        assertThat(oppgaveFraBase.getOppgaveId()).isEqualTo(oppgaveIdFraGSAK);
    }

    private long lagreOppgave(OppgaveBehandlingKobling oppgave) {
        return oppgaveBehandlingKoblingRepository.lagre(oppgave);
    }

    @Test
    public void skal_knytte_en_oppgave_til_en_behandling() {
        // Arrange
        String oppgaveIdFraGSAK = "IDFRAGSAK";
        OppgaveÅrsak behandleSøknad = OppgaveÅrsak.BEHANDLE_SAK;

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        final FamilieHendelseBuilder familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1)
            .medFødselsDato(LocalDate.now());
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Act
        OppgaveBehandlingKobling oppgave = new OppgaveBehandlingKobling(behandleSøknad, oppgaveIdFraGSAK, SAKSNUMMER, behandling.getId());
        lagreOppgave(oppgave);

        // Assert
        var kobling = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        assertThat(OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(OppgaveÅrsak.BEHANDLE_SAK, kobling)).isNotNull();
    }

    @Test
    public void skal_kunne_ferdigstille_en_eksisterende_oppgave() {
        // Arrange
        String oppgaveIdFraGSAK = "IDFRAGSAK";
        OppgaveÅrsak behandleSøknad = OppgaveÅrsak.BEHANDLE_SAK;
        String saksbehandler = "R160223";

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        final FamilieHendelseBuilder familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1)
            .medFødselsDato(LocalDate.now());
        Behandling behandling = scenario.lagre(repositoryProvider);

        OppgaveBehandlingKobling oppgave = new OppgaveBehandlingKobling(behandleSøknad, oppgaveIdFraGSAK, SAKSNUMMER, behandling.getId());
        Long id = lagreOppgave(oppgave);

        // Act
        var oppgaverFraBase = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        assertThat(oppgaverFraBase).hasSize(1);
        var oppgaveKoblingFraBase = oppgaverFraBase.get(0);
        oppgaveKoblingFraBase.ferdigstillOppgave(saksbehandler);
        lagreOppgave(oppgaveKoblingFraBase);

        OppgaveBehandlingKobling oppgaveHentetFraBasen = repository.hent(OppgaveBehandlingKobling.class, oppgaveKoblingFraBase.getId());
        assertThat(oppgaveHentetFraBasen.isFerdigstilt()).isTrue();
    }

}
