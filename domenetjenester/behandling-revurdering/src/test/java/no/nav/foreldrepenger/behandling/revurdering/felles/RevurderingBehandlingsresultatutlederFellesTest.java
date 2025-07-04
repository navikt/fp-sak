package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles.erAvslagPåAvslag;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;

@ExtendWith(JpaExtension.class)
class RevurderingBehandlingsresultatutlederFellesTest {

    private BehandlingRepository behandlingRepository;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        behandlingRepository = new BehandlingRepository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    void skal_ikke_gi_avslag_på_avslag() {
        // Arrange
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = lagRevurdering(originalBehandling);

        // Act
        var erAvslagPåAvslag = erAvslagPåAvslag(
            lagBehandlingsresultat(revurdering, BehandlingResultatType.INGEN_ENDRING,
                KonsekvensForYtelsen.INGEN_ENDRING, VilkårUtfallType.OPPFYLT),
            lagBehandlingsresultat(originalBehandling, BehandlingResultatType.INNVILGET,
                KonsekvensForYtelsen.UDEFINERT, VilkårUtfallType.OPPFYLT));

        // Assert
        assertThat(erAvslagPåAvslag).isFalse();
    }

    @Test
    void skal_gi_avslag_på_avslag() {
        // Arrange
        var originalBehandling = opprettOriginalBehandling();
        var revurdering = lagRevurdering(originalBehandling);

        // Act
        var erAvslagPåAvslag = erAvslagPåAvslag(
            lagBehandlingsresultat(revurdering, BehandlingResultatType.INGEN_ENDRING,
                KonsekvensForYtelsen.INGEN_ENDRING, VilkårUtfallType.IKKE_OPPFYLT),
            lagBehandlingsresultat(originalBehandling, BehandlingResultatType.AVSLÅTT, KonsekvensForYtelsen.UDEFINERT, VilkårUtfallType.IKKE_OPPFYLT));

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

    private Behandlingsresultat lagBehandlingsresultat(Behandling behandling, BehandlingResultatType resultatType,
                                                                 KonsekvensForYtelsen konsekvensForYtelsen, VilkårUtfallType utfallType) {
        var behandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(resultatType)
            .leggTilKonsekvensForYtelsen(konsekvensForYtelsen).buildFor(behandling);

        VilkårResultat.builder()
            .overstyrVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, utfallType,
                VilkårUtfallType.IKKE_OPPFYLT.equals(utfallType) ? Avslagsårsak.MANGLENDE_DOKUMENTASJON : Avslagsårsak.UDEFINERT)
            .buildFor(behandling);
        behandlingRepository.lagre(behandling.getBehandlingsresultat().getVilkårResultat(),
            behandlingRepository.taSkriveLås(behandling));

        return behandlingsresultat;
    }

}
