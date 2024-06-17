package no.nav.foreldrepenger.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;

@ExtendWith(JpaExtension.class)
class DekningsgradTjenesteTest {

    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    void skal_gi_endret_dekningsgrad_hvis_behandlingen_har_endret_dekningsgrad() {
        var førstegangs = ScenarioMorSøkerForeldrepenger.forFødsel().medOppgittDekningsgrad(Dekningsgrad._80).lagre(repositoryProvider);
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(førstegangs, BehandlingÅrsakType.RE_HENDELSE_DØD_BARN)
            .medOppgittDekningsgrad(Dekningsgrad._100)
            .lagre(repositoryProvider);

        var tjeneste = new DekningsgradTjeneste(repositoryProvider.getYtelsesFordelingRepository());

        assertThat(tjeneste.behandlingHarEndretDekningsgrad(BehandlingReferanse.fra(revurdering))).isTrue();
    }

    @Test
    void skal_gi_ikke_gi_endret_dekningsgrad_hvis_dekningsgrad_ikke_er_endret() {
        var førstegangs = ScenarioMorSøkerForeldrepenger.forFødsel().medOppgittDekningsgrad(Dekningsgrad._100).lagre(repositoryProvider);
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(førstegangs, BehandlingÅrsakType.RE_HENDELSE_DØD_BARN)
            .medOppgittDekningsgrad(Dekningsgrad._100)
            .lagre(repositoryProvider);

        var tjeneste = new DekningsgradTjeneste(repositoryProvider.getYtelsesFordelingRepository());

        assertThat(tjeneste.behandlingHarEndretDekningsgrad(BehandlingReferanse.fra(revurdering))).isFalse();
    }

}
