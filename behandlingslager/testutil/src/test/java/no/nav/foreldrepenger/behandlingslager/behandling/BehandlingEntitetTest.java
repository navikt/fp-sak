package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

class BehandlingEntitetTest extends EntityManagerAwareTest {

    private BehandlingRepository behandlingRepository;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    void skal_opprette_ny_behandling_på_ny_fagsak() {

        var behandling = opprettOgLagreBehandling();

        var alle = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(behandling.getFagsak().getId());

        assertThat(alle).hasSize(1);

        var første = alle.get(0);

        assertThat(første).isEqualTo(behandling);
    }

    private Behandling opprettOgLagreBehandling() {
        var scenarioMorSøkerEngangsstønad = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenarioMorSøkerEngangsstønad.medSøknadHendelse().medAntallBarn(1).medFødselsDato(LocalDate.now());
        return scenarioMorSøkerEngangsstønad.lagre(repositoryProvider);
    }

    @Test
    void skal_opprette_ny_behandling_på_fagsak_med_tidligere_behandling() {

        var behandling = opprettOgLagreBehandling();

        var behandling2 = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING).build();
        lagreBehandling(behandling2);

        var alle = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(behandling.getFagsak().getId());

        assertThat(alle).hasSize(2);

        var første = alle.get(0);
        var andre = alle.get(1);

        assertThat(første).isNotEqualTo(andre);
    }

    private void lagreBehandling(Behandling behandling) {
        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);
    }

    @Test
    void skal_opprette_ny_behandling_med_søknad() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));

        var behandling = scenario.lagre(repositoryProvider);

        var alle = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(behandling.getFagsak().getId());

        assertThat(alle).hasSize(1);

        var første = alle.get(0);
        var søknad = repositoryProvider.getSøknadRepository().hentSøknad(første.getId());
        var fhGrunnlag = repositoryProvider.getFamilieHendelseRepository().hentAggregat(første.getId());
        assertThat(søknad).isNotNull();
        assertThat(søknad.getSøknadsdato()).isEqualTo(LocalDate.now());
        assertThat(fhGrunnlag.getSøknadVersjon().getTerminbekreftelse()).isNotPresent();
        assertThat(fhGrunnlag.getSøknadVersjon().getBarna()).hasSize(1);
        assertThat(fhGrunnlag.getSøknadVersjon().getBarna().iterator().next().getFødselsdato()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    void skal_ikke_opprette_nytt_behandlingsgrunnlag_når_endring_skjer_på_samme_behandling_som_originalt_lagd_for() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now())
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("Lege legesen"));
        var behandling = scenario.lagre(repositoryProvider);

        var terminDato = LocalDate.now();

        lagreBehandling(behandling);
        var grunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        var grunnlag = grunnlagRepository.hentAggregat(behandling.getId());

        // Ny behandling gir samme grunnlag når det ikke er endringer
        var behandlingBuilder2 = Behandling.fraTidligereBehandling(behandling, BehandlingType.FØRSTEGANGSSØKNAD);
        var nyTerminDato = LocalDate.now().plusDays(1);

        var behandling2 = behandlingBuilder2.build();
        lagreBehandling(behandling2);
        grunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), behandling2.getId());
        var oppdatere = grunnlagRepository.opprettBuilderForSøknad(behandling.getId());
        oppdatere.medTerminbekreftelse(oppdatere.getTerminbekreftelseBuilder()
                .medTermindato(nyTerminDato)
                .medUtstedtDato(terminDato).medNavnPå("Lege navn"));
        grunnlagRepository.lagreSøknadHendelse(behandling2.getId(), oppdatere);

        var oppdatertGrunnlag = grunnlagRepository.hentAggregat(behandling2.getId());
        assertThat(oppdatertGrunnlag).isNotSameAs(grunnlag);

        assertThat(grunnlag.getGjeldendeVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato)).hasValue(terminDato);
        assertThat(oppdatertGrunnlag.getGjeldendeVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato))
                .hasValue(nyTerminDato);
    }
}
