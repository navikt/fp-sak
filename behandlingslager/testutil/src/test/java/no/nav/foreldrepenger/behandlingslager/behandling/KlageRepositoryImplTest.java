package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.Repository;

@RunWith(CdiRunner.class)
public class KlageRepositoryImplTest {


    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private Behandling behandling;
    private final EntityManager entityManager = repoRule.getEntityManager();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    
    @Inject
    private BehandlingRepository behandlingRepository;
    
    @Inject
    private KlageRepository klageRepository;

    @Test
    public void skal_slette_klage_resultat() {
        // Arrange
        Repository repository = repoRule.getRepository();
        var scenario = ScenarioKlageEngangsstønad.forAvvistNK(ScenarioFarSøkerEngangsstønad.forAdopsjon());
        behandling = scenario.lagre(repositoryProvider, klageRepository);
        entityManager.flush();

        // Asserting arrangement
        Long klageBehandlingId = behandling.getId();
        assertThat(klageRepository.hentKlageVurderingResultat(klageBehandlingId, KlageVurdertAv.NFP))
            .as("Mangler KlageVurderingResultat gitt av NFP").isPresent();
        assertThat(klageRepository.hentKlageVurderingResultat(klageBehandlingId, KlageVurdertAv.NK))
            .as("Mangler KlageVurderingResultat gitt av NK").isPresent();

        // Act
        klageRepository.slettKlageVurderingResultat(klageBehandlingId, KlageVurdertAv.NK);
        klageRepository.slettKlageVurderingResultat(klageBehandlingId, KlageVurdertAv.NFP);
        repository.flushAndClear();


        // Assert
        assertThat(klageRepository.hentKlageVurderingResultat(klageBehandlingId, KlageVurdertAv.NFP))
            .as("KlageVurderingResultat gitt av NFP ikke fjernet.").isNotPresent();
        assertThat(klageRepository.hentKlageVurderingResultat(klageBehandlingId, KlageVurdertAv.NK))
            .as("KlageVurderingResultat gitt av NK ikke fjernet.").isNotPresent();
    }

    @Test
    public void skal_slette_formkrav() {
        // Arrange
        Repository repository = repoRule.getRepository();
        var scenario = ScenarioKlageEngangsstønad.forAvvistNK(ScenarioFarSøkerEngangsstønad.forAdopsjon());
        behandling = scenario.lagre(repositoryProvider, klageRepository);
        entityManager.flush();

        // Asserting arrangement
        Behandling behandlingMedKlageVR = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(klageRepository.hentKlageFormkrav(behandlingMedKlageVR, KlageVurdertAv.NFP))
            .as("Mangler KlageVurderingResultat gitt av NFP").isPresent();

        // Act
        klageRepository.slettFormkrav(behandlingMedKlageVR, KlageVurdertAv.NFP);
        repository.flushAndClear();

        // Assert
        Behandling behandlingEtterSletting = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(klageRepository.hentKlageFormkrav(behandlingEtterSletting, KlageVurdertAv.NFP))
            .as("KlageVurderingResultat gitt av NFP ikke fjernet.").isNotPresent();
    }


    @Test
    public void skal_lagre_og_oppdatere_formkrav() {
        // Arrange
        Repository repository = repoRule.getRepository();
        var scenario = ScenarioKlageEngangsstønad.forAvvistNK(ScenarioFarSøkerEngangsstønad.forAdopsjon());
        behandling = scenario.lagre(repositoryProvider, klageRepository);
        entityManager.flush();

        KlageResultatEntitet klageResultat = klageRepository.hentKlageResultat(behandling);
        KlageFormkravEntitet.Builder builder1 = opprettFormkravBuilder(klageResultat, KlageVurdertAv.NK)
            .medBegrunnelse("Begrunnelse1");

        // Act
        klageRepository.lagreFormkrav(behandling, builder1);
        repository.flushAndClear();

        // Assert
        Optional<KlageFormkravEntitet> klageFormkrav = klageRepository.hentKlageFormkrav(behandling, KlageVurdertAv.NK);
        assertThat(klageFormkrav).as("Formkrav NK opprettet").isPresent();
        assertThat(klageFormkrav.get().hentBegrunnelse()).isEqualTo("Begrunnelse1");

        // Arrange
        KlageFormkravEntitet.Builder builder2 = opprettFormkravBuilder(klageResultat, KlageVurdertAv.NK)
            .medBegrunnelse("Begrunnelse2");

        // Act 2
        klageRepository.lagreFormkrav(behandling, builder2);
        repository.flushAndClear();

        // Assert
        Optional<KlageFormkravEntitet> klageFormkrav2 = klageRepository.hentKlageFormkrav(behandling, KlageVurdertAv.NK);
        assertThat(klageFormkrav2).as("Formkrav NK opprettet").isPresent();
        assertThat(klageFormkrav2.get().hentBegrunnelse()).isEqualTo("Begrunnelse2");

    }

    @Test
    public void skal_lagre_og_oppdatere_vurderingresultat() {
        // Arrange
        Repository repository = repoRule.getRepository();
        var scenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(ScenarioFarSøkerEngangsstønad.forAdopsjon());
        behandling = scenario.lagre(repositoryProvider, klageRepository);
        entityManager.flush();

        KlageResultatEntitet klageResultat = klageRepository.hentKlageResultat(behandling);
        KlageVurderingResultat.Builder builder1 = opprettVurderingResultat(klageResultat, KlageVurdertAv.NFP)
            .medBegrunnelse("Begrunnelse1");

        // Act
        klageRepository.lagreVurderingsResultat(behandling, builder1);
        repository.flushAndClear();

        Long klageBehandlingId = behandling.getId();
        // Assert
        Optional<KlageVurderingResultat> klageVurderingResultat = klageRepository.hentKlageVurderingResultat(klageBehandlingId, KlageVurdertAv.NFP);
        assertThat(klageVurderingResultat).as("Formkrav NK opprettet").isPresent();
        assertThat(klageVurderingResultat.get().getBegrunnelse()).isEqualTo("Begrunnelse1");

        // Arrange
        KlageVurderingResultat.Builder builder2 = opprettVurderingResultat(klageResultat, KlageVurdertAv.NFP)
            .medKlageVurdering(KlageVurdering.AVVIS_KLAGE)
            .medKlageAvvistÅrsak(KlageAvvistÅrsak.KLAGE_UGYLDIG)
            .medBegrunnelse("Begrunnelse2");

        // Act
        klageRepository.lagreVurderingsResultat(behandling, builder2);
        repository.flushAndClear();

        // Assert
        Optional<KlageVurderingResultat> klageVurderingResultat2 = klageRepository.hentKlageVurderingResultat(klageBehandlingId, KlageVurdertAv.NFP);
        assertThat(klageVurderingResultat2).as("Formkrav NK opprettet").isPresent();
        assertThat(klageVurderingResultat2.get().getBegrunnelse()).isEqualTo("Begrunnelse2");
    }

    @Test
    public void skal_hente_formkrav_fra_ka_når_nfp_og_ka_har_vurdert() {
        // Pre Arrange
        Repository repository = repoRule.getRepository();
        var scenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(ScenarioFarSøkerEngangsstønad.forAdopsjon());
        behandling = scenario.lagre(repositoryProvider, klageRepository);
        entityManager.flush();

        // Arrange
        KlageResultatEntitet klageResultat = klageRepository.hentKlageResultat(behandling);
        KlageFormkravEntitet.Builder builderNfp = opprettFormkravBuilder(klageResultat, KlageVurdertAv.NFP)
            .medBegrunnelse("Begrunnelse1");

        // Act
        klageRepository.lagreFormkrav(behandling,builderNfp);
        repository.flushAndClear();

        // Assert
        Optional<KlageFormkravEntitet> klageFormkravNfp = klageRepository.hentGjeldendeKlageFormkrav(behandling);
        assertThat(klageFormkravNfp).as("Formkrav Nfp opprettet").isPresent();
        assertThat(klageFormkravNfp.get().getKlageVurdertAv()).isEqualTo(KlageVurdertAv.NFP);

        // Arrange
        KlageFormkravEntitet.Builder builderKa = opprettFormkravBuilder(klageResultat, KlageVurdertAv.NK)
            .medBegrunnelse("Begrunnelse2");

        // Act
        klageRepository.lagreFormkrav(behandling,builderKa);
        repository.flushAndClear();

        // Assert
        Optional<KlageFormkravEntitet> klageFormkravKa = klageRepository.hentGjeldendeKlageFormkrav(behandling);
        assertThat(klageFormkravKa).as("Formkrav opprettet").isPresent();
        assertThat(klageFormkravKa.get().getKlageVurdertAv()).isEqualTo(KlageVurdertAv.NK);
    }


    private KlageVurderingResultat.Builder opprettVurderingResultat(KlageResultatEntitet klageResultat, KlageVurdertAv klageVurdertAv) {
        return new KlageVurderingResultat.Builder()
            .medKlageResultat(klageResultat)
            .medKlageVurdertAv(klageVurdertAv)
            .medKlageVurdering(KlageVurdering.MEDHOLD_I_KLAGE)
            .medKlageMedholdÅrsak(KlageMedholdÅrsak.ULIK_VURDERING);
    }


    private KlageFormkravEntitet.Builder opprettFormkravBuilder(KlageResultatEntitet klageResultat, KlageVurdertAv klageVurdertAv) {
        return new KlageFormkravEntitet.Builder()
            .medErKlagerPart(true)
            .medErFristOverholdt(true)
            .medErKonkret(true)
            .medErSignert(true)
            .medGjelderVedtak(true)
            .medBegrunnelse("Begrunnelse")
            .medKlageResultat(klageResultat)
            .medKlageVurdertAv(klageVurdertAv);
    }
}
