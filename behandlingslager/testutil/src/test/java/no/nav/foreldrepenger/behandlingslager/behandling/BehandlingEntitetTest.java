package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class BehandlingEntitetTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private Repository repository = repoRule.getRepository();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Before
    public void setup() {
    }

    @Test
    public void skal_opprette_ny_behandling_på_ny_fagsak() {

        Behandling behandling = opprettOgLagreBehandling();

        List<Behandling> alle = repository.hentAlle(Behandling.class);

        assertThat(alle).hasSize(1);

        Behandling første = alle.get(0);

        assertThat(første).isEqualTo(behandling);
    }

    private Behandling opprettOgLagreBehandling() {
        final ScenarioMorSøkerEngangsstønad scenarioMorSøkerEngangsstønad = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenarioMorSøkerEngangsstønad.medSøknadHendelse().medAntallBarn(1).medFødselsDato(LocalDate.now());
        return scenarioMorSøkerEngangsstønad.lagre(repositoryProvider);
    }

    @Test
    public void skal_opprette_ny_behandling_på_fagsak_med_tidligere_behandling() {

        Behandling behandling = opprettOgLagreBehandling();

        Behandling behandling2 = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING).build();
        lagreBehandling(behandling2);

        List<Behandling> alle = repository.hentAlle(Behandling.class);

        assertThat(alle).hasSize(2);

        Behandling første = alle.get(0);
        Behandling andre = alle.get(1);

        assertThat(første).isNotEqualTo(andre);
    }

    private void lagreBehandling(Behandling behandling) {
        BehandlingLås lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);
    }

    @Test
    public void skal_opprette_ny_behandling_med_søknad() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medFødselAdopsjonsdato(Collections.singletonList(LocalDate.now().plusDays(1)));

        scenario.lagre(repositoryProvider);

        List<Behandling> alle = repository.hentAlle(Behandling.class);

        assertThat(alle).hasSize(1);

        Behandling første = alle.get(0);
        final SøknadEntitet søknad = repositoryProvider.getSøknadRepository().hentSøknad(første);
        final var fhGrunnlag = repositoryProvider.getFamilieHendelseRepository().hentAggregat(første.getId());
        assertThat(søknad).isNotNull();
        assertThat(søknad.getSøknadsdato()).isEqualTo(LocalDate.now());
        assertThat(fhGrunnlag.getSøknadVersjon().getTerminbekreftelse()).isNotPresent();
        assertThat(fhGrunnlag.getSøknadVersjon().getBarna()).hasSize(1);
        assertThat(fhGrunnlag.getSøknadVersjon().getBarna().iterator().next().getFødselsdato())
            .isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    public void skal_ikke_opprette_nytt_behandlingsgrunnlag_når_endring_skjer_på_samme_behandling_som_originalt_lagd_for() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now()).medUtstedtDato(LocalDate.now()).medNavnPå("Lege legesen"));
        Behandling behandling = scenario.lagre(repositoryProvider);

        LocalDate terminDato = LocalDate.now();

        lagreBehandling(behandling);
        final FamilieHendelseRepository grunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        final FamilieHendelseGrunnlagEntitet grunnlag = grunnlagRepository.hentAggregat(behandling.getId());

        // Ny behandling gir samme grunnlag når det ikke er endringer
        Behandling.Builder behandlingBuilder2 = Behandling.fraTidligereBehandling(behandling, BehandlingType.FØRSTEGANGSSØKNAD);
        LocalDate nyTerminDato = LocalDate.now().plusDays(1);

        Behandling behandling2 = behandlingBuilder2.build();
        lagreBehandling(behandling2);
        grunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), behandling2.getId());
        final FamilieHendelseBuilder oppdatere = grunnlagRepository.opprettBuilderFor(behandling);
        oppdatere.medTerminbekreftelse(oppdatere.getTerminbekreftelseBuilder()
            .medTermindato(nyTerminDato)
            .medUtstedtDato(terminDato).medNavnPå("Lege navn"));
        grunnlagRepository.lagre(behandling2, oppdatere);

        final FamilieHendelseGrunnlagEntitet oppdatertGrunnlag = grunnlagRepository.hentAggregat(behandling2.getId());
        assertThat(oppdatertGrunnlag).isNotSameAs(grunnlag);

        assertThat(grunnlag.getGjeldendeVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato)).hasValue(terminDato);
        assertThat(oppdatertGrunnlag.getGjeldendeVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato)).hasValue(nyTerminDato);
    }
}
