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
public class AnkeRepositoryTest {

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
        AnkeResultatEntitet hentetAnkeResultat = ankeRepository.hentEllerOpprettAnkeResultat(ankeBehandling.getId());

        // Assert
        assertThat(hentetAnkeResultat.getAnkeBehandlingId()).isEqualTo(ankeBehandling.getId());
    }

    @Test
    public void skal_lagre_og_hente_ankevurderingResultat() {
        // Arrange
        Repository repository = repoRule.getRepository();
        ScenarioAnkeEngangsstønad scenario = ScenarioAnkeEngangsstønad.forAvvistAnke(ScenarioFarSøkerEngangsstønad.forAdopsjon());
        ankeBehandling = scenario.lagre(repositoryProvider);
        entityManager.flush();

        AnkeResultatEntitet ankeResultat = ankeRepository.hentEllerOpprettAnkeResultat(ankeBehandling.getId());
        AnkeVurderingResultatEntitet.Builder ankeVurderingResultatBuilder = opprettVurderingResultat(ankeResultat)
            .medAnkeResultat(ankeResultat)
            .medBegrunnelse("Begrunnelse1")
            .medFritekstTilBrev("Fritekstbrev1");

        // Act
        Long ankeVurderingResultatId = ankeRepository.lagreVurderingsResultat(ankeBehandling.getId(), ankeVurderingResultatBuilder.build());
        repository.flushAndClear();
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
}
