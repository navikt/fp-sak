package no.nav.foreldrepenger.web.app.tjenester.gosys;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class GosysRestTjenesteTest extends EntityManagerAwareTest {

    private GosysRestTjeneste gosysRestTjeneste; // objektet vi tester

    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        gosysRestTjeneste = new GosysRestTjeneste(repositoryProvider);
    }

    @Test
    public void skal_konvertere_fagsak_for_engangsstønad_ved_fødsel_til_ekstern_representasjon() {
        final var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSaksnummer(new Saksnummer("1337"));
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        final var behandling = scenario.lagre(repositoryProvider);

        var respons = gosysRestTjeneste.lagResponse(Collections.singletonList(behandling.getFagsak()));

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

        var respons = gosysRestTjeneste.lagResponse(Collections.singletonList(behandling.getFagsak()));

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
        var respons = gosysRestTjeneste.lagResponse(Collections.singletonList(fagsak));

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
        var respons = gosysRestTjeneste.lagResponse(Collections.singletonList(fagsak));

        assertThat(respons.sakListe()).hasSize(1);
        var sak = respons.sakListe().get(0);
        assertThat(sak.behandlingstema().value()).isEqualTo("ab0126"); // betyr svangerskapspenger
        assertThat(sak.behandlingstema().termnavn()).isEqualTo("Svangerskapspenger");
        assertThat(sak.sakId()).isEqualTo("1339");
    }
}
