package no.nav.foreldrepenger.behandling.revurdering.felles;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.behandlingslager.behandling.*;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
class FastsettBehandlingsresultatVedAvslagPåAvslagTest {

    private BehandlingRepository behandlingRepository;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        behandlingRepository = new BehandlingRepository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    void skal_ikke_gi_avslag_på_avslag() {
        // Arrange
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = lagRevurdering(originalBehandling);

        // Act
        var erAvslagPåAvslag = FastsettBehandlingsresultatVedAvslagPåAvslag.vurder(
                lagBehandlingsresultat(revurdering, BehandlingResultatType.INGEN_ENDRING,
                        KonsekvensForYtelsen.INGEN_ENDRING),
                lagBehandlingsresultat(originalBehandling, BehandlingResultatType.INNVILGET,
                        KonsekvensForYtelsen.UDEFINERT),
                originalBehandling.getType());

        // Assert
        assertThat(erAvslagPåAvslag).isFalse();
    }

    @Test
    void skal_gi_avslag_på_avslag() {
        // Arrange
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = lagRevurdering(originalBehandling);

        // Act
        var erAvslagPåAvslag = FastsettBehandlingsresultatVedAvslagPåAvslag.vurder(
                lagBehandlingsresultat(revurdering, BehandlingResultatType.INGEN_ENDRING,
                        KonsekvensForYtelsen.INGEN_ENDRING),
                lagBehandlingsresultat(originalBehandling, BehandlingResultatType.AVSLÅTT, KonsekvensForYtelsen.UDEFINERT),
                originalBehandling.getType());

        // Assert
        assertThat(erAvslagPåAvslag).isTrue();
    }

    private Behandling lagRevurdering(Behandling originalBehandling) {
        var revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(
                        BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                                .medManueltOpprettet(true)
                                .medOriginalBehandlingId(originalBehandling.getId()))
                .build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));
        return revurdering;
    }

    private Behandling opprettOriginalBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var originalBehandling = scenario.lagre(repositoryProvider);
        originalBehandling.avsluttBehandling();
        return originalBehandling;
    }

    private Optional<Behandlingsresultat> lagBehandlingsresultat(Behandling behandling,
            BehandlingResultatType resultatType,
            KonsekvensForYtelsen konsekvensForYtelsen) {
        var behandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(resultatType)
                .leggTilKonsekvensForYtelsen(konsekvensForYtelsen).buildFor(behandling);

        VilkårResultat.builder().medVilkårResultatType(VilkårResultatType.AVSLÅTT).buildFor(behandling);
        behandlingRepository.lagre(behandling.getBehandlingsresultat().getVilkårResultat(),
                behandlingRepository.taSkriveLås(behandling));

        return Optional.of(behandlingsresultat);
    }
}
