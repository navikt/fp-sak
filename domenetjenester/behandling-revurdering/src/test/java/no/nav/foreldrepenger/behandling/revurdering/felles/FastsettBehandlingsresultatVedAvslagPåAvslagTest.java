package no.nav.foreldrepenger.behandling.revurdering.felles;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class FastsettBehandlingsresultatVedAvslagPåAvslagTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();

    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private Behandling originalBehandling;
    private Behandling revurdering;

    @Before
    public void setUp() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        originalBehandling = scenario.lagre(repositoryProvider);
        originalBehandling.avsluttBehandling();
        revurdering = lagRevurdering(originalBehandling);
    }

    @Test
    public void skal_ikke_gi_avslag_på_avslag() {
        // Arrange

        // Act
        boolean erAvslagPåAvslag = FastsettBehandlingsresultatVedAvslagPåAvslag.vurder(
            lagBehandlingsresultat(revurdering, BehandlingResultatType.INGEN_ENDRING, KonsekvensForYtelsen.INGEN_ENDRING),
            lagBehandlingsresultat(originalBehandling, BehandlingResultatType.INNVILGET, KonsekvensForYtelsen.UDEFINERT), originalBehandling.getType());

        // Assert
        assertThat(erAvslagPåAvslag).isFalse();
    }

    @Test
    public void skal_gi_avslag_på_avslag() {
        // Arrange

        // Act
        boolean erAvslagPåAvslag = FastsettBehandlingsresultatVedAvslagPåAvslag.vurder(
            lagBehandlingsresultat(revurdering, BehandlingResultatType.INGEN_ENDRING, KonsekvensForYtelsen.INGEN_ENDRING),
            lagBehandlingsresultat(originalBehandling, BehandlingResultatType.AVSLÅTT, KonsekvensForYtelsen.UDEFINERT), originalBehandling.getType());

        // Assert
        assertThat(erAvslagPåAvslag).isTrue();
    }

    private Behandling lagRevurdering(Behandling originalBehandling) {
        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(
                BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                    .medManueltOpprettet(true)
                    .medOriginalBehandling(originalBehandling))
            .build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));
        return revurdering;
    }

    private Optional<Behandlingsresultat> lagBehandlingsresultat(Behandling behandling, BehandlingResultatType resultatType, KonsekvensForYtelsen konsekvensForYtelsen) {
        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(resultatType)
            .leggTilKonsekvensForYtelsen(konsekvensForYtelsen).buildFor(behandling);

        VilkårResultat.builder().medVilkårResultatType(VilkårResultatType.AVSLÅTT).buildFor(behandling);
        behandlingRepository.lagre(behandling.getBehandlingsresultat().getVilkårResultat(), behandlingRepository.taSkriveLås(behandling));

        return Optional.of(behandlingsresultat);
    }
}
