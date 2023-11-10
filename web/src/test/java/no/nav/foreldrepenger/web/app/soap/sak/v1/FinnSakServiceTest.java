package no.nav.foreldrepenger.web.app.soap.sak.v1;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@Execution(ExecutionMode.SAME_THREAD)
class FinnSakServiceTest extends EntityManagerAwareTest {

    private FinnSakService finnSakService; // objektet vi tester

    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        finnSakService = new FinnSakService(repositoryProvider);
    }

    @Test
    void skal_konvertere_fagsak_for_engangsstønad_ved_fødsel_til_ekstern_representasjon() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSaksnummer(new Saksnummer("1337"));
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var behandling = scenario.lagre(repositoryProvider);

        var respons = finnSakService.lagResponse(Collections.singletonList(behandling.getFagsak()));

        assertThat(respons.getSakListe()).hasSize(1);
        var sak = respons.getSakListe().get(0);
        assertThat(sak.getBehandlingstema().getValue()).isEqualTo("ab0050"); // betyr engangsstønad ved fødsel
        assertThat(sak.getBehandlingstema().getTermnavn()).isEqualTo("Engangsstønad ved fødsel");
        assertThat(sak.getSakId()).isEqualTo("1337");
    }

    @Test
    void skal_konvertere_fagsak_for_engangsstønad_ved_adopsjon_til_ekstern_representasjon() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSaksnummer(new Saksnummer("1337"));
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));
        var behandling = scenario.lagre(repositoryProvider);

        var respons = finnSakService.lagResponse(Collections.singletonList(behandling.getFagsak()));

        assertThat(respons.getSakListe()).hasSize(1);
        var sak = respons.getSakListe().get(0);
        assertThat(sak.getBehandlingstema().getValue()).isEqualTo("ab0027"); // betyr engangsstønad ved adopsjon
        assertThat(sak.getBehandlingstema().getTermnavn()).isEqualTo("Engangsstønad ved adopsjon");
        assertThat(sak.getSakId()).isEqualTo("1337");
    }

    @Test
    void skal_konvertere_fagsak_uten_behandlinger_til_ekstern_representasjon() {
        var navBruker = new NavBrukerBuilder()
                .medAktørId(AktørId.dummy())
                .medKjønn(NavBrukerKjønn.KVINNE)
                .build();

        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker, null, new Saksnummer("1338"));
        fagsak.setId(1L);
        var respons = finnSakService.lagResponse(Collections.singletonList(fagsak));

        assertThat(respons.getSakListe()).hasSize(1);
        var sak = respons.getSakListe().get(0);
        assertThat(sak.getBehandlingstema().getValue()).isEqualTo("ab0326"); // betyr foreldrepenger
        assertThat(sak.getBehandlingstema().getTermnavn()).isEqualTo("Foreldrepenger");
        assertThat(sak.getSakId()).isEqualTo("1338");
    }

    @Test
    void skal_konvertere_svangerskapspenger_fagsak_uten_behandlinger_til_ekstern_representasjon() {
        var navBruker = new NavBrukerBuilder()
                .medAktørId(AktørId.dummy())
                .medKjønn(NavBrukerKjønn.KVINNE)
                .build();

        var fagsak = Fagsak.opprettNy(FagsakYtelseType.SVANGERSKAPSPENGER, navBruker, null, new Saksnummer("1339"));
        fagsak.setId(1L);
        var respons = finnSakService.lagResponse(Collections.singletonList(fagsak));

        assertThat(respons.getSakListe()).hasSize(1);
        var sak = respons.getSakListe().get(0);
        assertThat(sak.getBehandlingstema().getValue()).isEqualTo("ab0126"); // betyr svangerskapspenger
        assertThat(sak.getBehandlingstema().getTermnavn()).isEqualTo("Svangerskapspenger");
        assertThat(sak.getSakId()).isEqualTo("1339");
    }

}
