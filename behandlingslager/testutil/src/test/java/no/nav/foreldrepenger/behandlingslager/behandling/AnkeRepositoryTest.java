package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioAnkeEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.JpaExtension;

@ExtendWith(JpaExtension.class)
class AnkeRepositoryTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private AnkeRepository ankeRepository;

    @BeforeEach
    void setup(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ankeRepository = new AnkeRepository(entityManager);
    }

    @Test
    void skal_legge_til_og_hente_ankeresultat() {
        var ankeBehandling = opprettBehandling();

        // Act
        var hentetAnkeResultat = ankeRepository.hentEllerOpprettAnkeResultat(ankeBehandling.getId());

        // Assert
        assertThat(hentetAnkeResultat.getAnkeBehandlingId()).isEqualTo(ankeBehandling.getId());
    }

    @Test
    void skal_lagre_og_hente_ankevurderingResultat() {
        // Arrange
        var scenario = ScenarioAnkeEngangsstønad.forAvvistAnke(ScenarioFarSøkerEngangsstønad.forAdopsjon());
        var ankeBehandling = scenario.lagre(repositoryProvider, true);

        var ankeResultat = ankeRepository.hentEllerOpprettAnkeResultat(ankeBehandling.getId());
        var ankeVurderingResultatBuilder = opprettVurderingResultat(ankeResultat)
            .medAnkeResultat(ankeResultat);

        // Act
        ankeRepository.lagreVurderingsResultat(ankeBehandling.getId(), ankeVurderingResultatBuilder.build());
        var hentetAnkeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(ankeBehandling.getId());

        // Assert
        assertThat(hentetAnkeVurderingResultat).isNotNull();
        assertThat(hentetAnkeVurderingResultat.get().getAnkeResultat()).isNotNull();
        assertThat(hentetAnkeVurderingResultat.get().getAnkeVurdering()).isEqualTo(AnkeVurdering.ANKE_OMGJOER);
    }

    @Test
    void settPåAnketKlageBehandling() {
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        var ankeBehandling = scenario.lagre(repositoryProvider);
        var scenario2 = ScenarioFarSøkerEngangsstønad.forFødsel();
        var påAnketKlageBehandling = scenario2.lagre(repositoryProvider);
        ankeRepository.settPåAnketKlageBehandling(ankeBehandling.getId(), påAnketKlageBehandling.getId());

        var påanketBehandlingId = ankeRepository.hentAnkeResultat(ankeBehandling.getId()).flatMap(AnkeResultatEntitet::getPåAnketKlageBehandlingId);
        assertThat(påanketBehandlingId)
            .isPresent()
            .contains(påAnketKlageBehandling.getId());
    }

    private AnkeVurderingResultatEntitet.Builder opprettVurderingResultat(AnkeResultatEntitet ankeResultat) {
        return AnkeVurderingResultatEntitet.builder()
            .medAnkeResultat(ankeResultat)
            .medAnkeVurdering(AnkeVurdering.ANKE_OMGJOER);
    }

    private Behandling opprettBehandling() {
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        return scenario.lagre(repositoryProvider);
    }
}
