package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioAnkeEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.Repository;

@RunWith(CdiRunner.class)
public class AnkeRepositoryImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private Behandling ankeBehandling;
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);

    @Inject
    private AnkeRepository ankeRepository;
    
    @Before
    public void setup() {
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        ankeBehandling = scenario.lagre(repositoryProvider);
        entityManager.flush();
    }

    @Test
    public void skal_legge_til_og_hente_ankeresultat() {

        // Act
        ankeRepository.leggTilAnkeResultat(ankeBehandling);
        Optional<AnkeResultatEntitet> hentetAnkeResultat = ankeRepository.hentAnkeResultat(ankeBehandling);

        // Assert
        assertThat(hentetAnkeResultat).isPresent();
        assertThat(hentetAnkeResultat.get().getAnkeBehandling()).isEqualTo(ankeBehandling);
    }

    @Test
    public void skal_lagre_og_hente_ankevurderingResultat() {
        // Arrange
        Repository repository = repoRule.getRepository();
        ScenarioAnkeEngangsstønad scenario = ScenarioAnkeEngangsstønad.forAvvistAnke(ScenarioFarSøkerEngangsstønad.forAdopsjon());
        ankeBehandling = scenario.lagre(repositoryProvider);
        entityManager.flush();

        ankeRepository.leggTilAnkeResultat(ankeBehandling);
        AnkeResultatEntitet ankeResultat = ankeRepository.hentAnkeResultat(ankeBehandling).get();
        AnkeVurderingResultatEntitet.Builder ankeVurderingResultatBuilder = opprettVurderingResultat(ankeResultat)
            .medBegrunnelse("Begrunnelse1")
            .medFritekstTilBrev("Fritekstbrev1");

        // Act
        Long ankeVurderingResultatId = ankeRepository.lagreVurderingsResultat(ankeBehandling, ankeVurderingResultatBuilder);
        repository.flushAndClear();
        assertThat(ankeVurderingResultatId).isNotNull();
        Optional<AnkeVurderingResultatEntitet> hentetAnkeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(ankeBehandling.getId());

        // Assert
        assertThat(hentetAnkeVurderingResultat).isNotNull();
        assertThat(hentetAnkeVurderingResultat.get().getAnkeResultat()).isNotNull();
        assertThat(hentetAnkeVurderingResultat.get().getAnkeVurdering()).isEqualTo(AnkeVurdering.ANKE_OMGJOER);
    }

    @Test
    public void skal_kunne_slette_ankevurderingResultat() {
        // Arrange
        Repository repository = repoRule.getRepository();
        ScenarioAnkeEngangsstønad scenario = ScenarioAnkeEngangsstønad.forOpphevOgHjemsende(ScenarioFarSøkerEngangsstønad.forAdopsjon());
        ankeBehandling = scenario.lagre(repositoryProvider);

        ankeRepository.leggTilAnkeResultat(ankeBehandling);
        entityManager.flush();

        AnkeResultatEntitet ankeResultat = ankeRepository.hentAnkeResultat(ankeBehandling).get();
        AnkeVurderingResultatEntitet.Builder ankeVurderingResultatBuilder = opprettVurderingResultat(ankeResultat)
            .medBegrunnelse("Begrunnelse1");

        // Act
        Long ankeVurderingResultatId = ankeRepository.lagreVurderingsResultat(ankeBehandling, ankeVurderingResultatBuilder);
        repository.flushAndClear();
        assertThat(ankeVurderingResultatId).isNotNull();
        ankeRepository.slettAnkeVurderingResultat(ankeBehandling.getId());

        // Assert
        assertThat(ankeRepository.hentAnkeVurderingResultat(ankeBehandling.getId())).isEmpty();
    }

    @Test
    public void settPåAnketBehandling() {
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        Behandling ankeBehandling = scenario.lagre(repositoryProvider);
        ScenarioFarSøkerEngangsstønad scenario2 = ScenarioFarSøkerEngangsstønad.forFødsel();
        Behandling påAnketBehandling = scenario2.lagre(repositoryProvider);
        ankeRepository.leggTilAnkeResultat(ankeBehandling);
        ankeRepository.settPåAnketBehandling(ankeBehandling, påAnketBehandling);
    }

    private AnkeVurderingResultatEntitet.Builder opprettVurderingResultat(AnkeResultatEntitet ankeResultat) {
        return new AnkeVurderingResultatEntitet.Builder()
            .medAnkeResultat(ankeResultat)
            .medAnkeVurdering(AnkeVurdering.ANKE_OMGJOER)
            .medAnkeOmgjørÅrsak(AnkeOmgjørÅrsak.ULIK_VURDERING);
    }
}
