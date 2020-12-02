package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndringBasertPåKonsekvenserForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class RevurderingEndringTest {

    private final RevurderingEndring revurderingEndring = new no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.RevurderingEndring();

    private BehandlingRepository behandlingRepository;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    void setup(EntityManager entityManager) {
        behandlingRepository = new BehandlingRepository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    public void jaHvisRevurderingMedUendretUtfall() {
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = opprettRevurdering(originalBehandling);
        Behandlingsresultat.builder()
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
                .leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.INGEN_ENDRING)
                .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        assertThat(revurderingEndring.erRevurderingMedUendretUtfall(revurdering)).isTrue();
        assertThat(revurderingEndring.erRevurderingMedUendretUtfall(revurdering, null)).isTrue();
    }

    private Behandling opprettRevurdering(Behandling originalBehandling) {
        var revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_MANGLER_FØDSEL)
                        .medOriginalBehandlingId(originalBehandling.getId()))
                .build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);
        return revurdering;
    }

    @Test
    public void kasterFeilHvisRevurderingMedUendretUtfallOgOpphørAvYtelsen() {
        // Arrange
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = opprettRevurdering(originalBehandling);
        Behandlingsresultat.builder()
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
                .leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.INGEN_ENDRING)
                .leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER)
                .buildFor(revurdering);

        // Act
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        // Assert
        assertThatThrownBy(() -> revurderingEndring.erRevurderingMedUendretUtfall(revurdering))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(RevurderingEndringBasertPåKonsekvenserForYtelsen.UTVIKLERFEIL_INGEN_ENDRING_SAMMEN);
        assertThatThrownBy(() -> revurderingEndring.erRevurderingMedUendretUtfall(revurdering, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(RevurderingEndringBasertPåKonsekvenserForYtelsen.UTVIKLERFEIL_INGEN_ENDRING_SAMMEN);
    }

    @Test
    public void neiHvisRevurderingMedEndring() {
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = opprettRevurdering(originalBehandling);
        Behandlingsresultat.builder()
                .medBehandlingResultatType(BehandlingResultatType.FORELDREPENGER_ENDRET)
                .leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.ENDRING_I_BEREGNING)
                .leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.ENDRING_I_UTTAK)
                .buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        assertThat(revurderingEndring.erRevurderingMedUendretUtfall(revurdering)).isFalse();
    }

    @Test
    public void neiHvisRevurderingMedOpphør() {
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = opprettRevurdering(originalBehandling);
        Behandlingsresultat.builder()
                .medBehandlingResultatType(BehandlingResultatType.OPPHØR)
                .leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER)
                .buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        assertThat(revurderingEndring.erRevurderingMedUendretUtfall(revurdering)).isFalse();
    }

    @Test
    public void neiHvisFørstegangsbehandling() {
        var originalBehandling = opprettOriginalBehandling();
        assertThat(revurderingEndring.erRevurderingMedUendretUtfall(originalBehandling)).isFalse();
    }

    private Behandling opprettOriginalBehandling() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger
                .forFødsel()
                .medDefaultBekreftetTerminbekreftelse();
        Behandling originalBehandling = scenario.lagre(repositoryProvider);
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(originalBehandling);
        behandlingRepository.lagre(originalBehandling, behandlingLås);
        return originalBehandling;
    }
}
