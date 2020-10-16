package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioAnkeEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class AnkeRepositoryTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private AnkeRepository ankeRepository;

    @BeforeEach
    void setup() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ankeRepository = new AnkeRepository(entityManager);
    }

    @Test
    public void skal_legge_til_og_hente_ankeresultat() {
        var ankeBehandling = opprettBehandling();

        // Act
        AnkeResultatEntitet hentetAnkeResultat = ankeRepository.hentEllerOpprettAnkeResultat(ankeBehandling.getId());

        // Assert
        assertThat(hentetAnkeResultat.getAnkeBehandlingId()).isEqualTo(ankeBehandling.getId());
    }

    @Test
    public void skal_lagre_og_hente_ankevurderingResultat() {
        // Arrange
        ScenarioAnkeEngangsstønad scenario = ScenarioAnkeEngangsstønad.forAvvistAnke(ScenarioFarSøkerEngangsstønad.forAdopsjon());
        var ankeBehandling = scenario.lagre(repositoryProvider);

        AnkeResultatEntitet ankeResultat = ankeRepository.hentEllerOpprettAnkeResultat(ankeBehandling.getId());
        AnkeVurderingResultatEntitet.Builder ankeVurderingResultatBuilder = opprettVurderingResultat(ankeResultat)
            .medAnkeResultat(ankeResultat)
            .medBegrunnelse("Begrunnelse1")
            .medFritekstTilBrev("Fritekstbrev1");

        // Act
        Long ankeVurderingResultatId = ankeRepository.lagreVurderingsResultat(ankeBehandling.getId(), ankeVurderingResultatBuilder.build());
        assertThat(ankeVurderingResultatId).isNotNull();
        Optional<AnkeVurderingResultatEntitet> hentetAnkeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(ankeBehandling.getId());

        // Assert
        assertThat(hentetAnkeVurderingResultat).isNotNull();
        assertThat(hentetAnkeVurderingResultat.get().getAnkeResultat()).isNotNull();
        assertThat(hentetAnkeVurderingResultat.get().getAnkeVurdering()).isEqualTo(AnkeVurdering.ANKE_OMGJOER);
    }

    @Test
    public void settPåAnketBehandling() {
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        Behandling ankeBehandling = scenario.lagre(repositoryProvider);
        ScenarioFarSøkerEngangsstønad scenario2 = ScenarioFarSøkerEngangsstønad.forFødsel();
        Behandling påAnketBehandling = scenario2.lagre(repositoryProvider);
        ankeRepository.settPåAnketBehandling(ankeBehandling.getId(), påAnketBehandling.getId());
    }

    private AnkeVurderingResultatEntitet.Builder opprettVurderingResultat(AnkeResultatEntitet ankeResultat) {
        return AnkeVurderingResultatEntitet.builder()
            .medAnkeResultat(ankeResultat)
            .medAnkeVurdering(AnkeVurdering.ANKE_OMGJOER)
            .medAnkeOmgjørÅrsak(AnkeOmgjørÅrsak.ULIK_VURDERING);
    }

    private Behandling opprettBehandling() {
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        return scenario.lagre(repositoryProvider);
    }
}
