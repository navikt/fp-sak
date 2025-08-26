package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

import java.time.LocalDate;

class KlageRepositoryTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private KlageRepository klageRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        klageRepository = new KlageRepository(entityManager);
    }

    @Test
    void skal_lagre_og_oppdatere_formkrav() {
        // Arrange
        var entityManager = getEntityManager();
        var scenario = ScenarioKlageEngangsstønad.forAvvistNK(ScenarioFarSøkerEngangsstønad.forAdopsjon());
        var behandling = scenario.lagre(repositoryProvider, klageRepository);
        entityManager.flush();

        var klageResultat = klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
        var builder1 = opprettFormkravBuilder(klageResultat, KlageVurdertAv.NK)
            .medBegrunnelse("Begrunnelse1");

        // Act
        klageRepository.lagreFormkrav(behandling, builder1);
        entityManager.flush();
        entityManager.clear();

        // Assert
        var klageFormkrav = klageRepository.hentKlageFormkrav(behandling.getId(), KlageVurdertAv.NK);
        assertThat(klageFormkrav).as("Formkrav NK opprettet").isPresent();
        assertThat(klageFormkrav.get().hentBegrunnelse()).isEqualTo("Begrunnelse1");

        // Arrange
        var builder2 = opprettFormkravBuilder(klageResultat, KlageVurdertAv.NK)
            .medBegrunnelse("Begrunnelse2");

        // Act 2
        klageRepository.lagreFormkrav(behandling, builder2);
        entityManager.flush();
        entityManager.clear();

        // Assert
        var klageFormkrav2 = klageRepository.hentKlageFormkrav(behandling.getId(), KlageVurdertAv.NK);
        assertThat(klageFormkrav2).as("Formkrav NK opprettet").isPresent();
        assertThat(klageFormkrav2.get().hentBegrunnelse()).isEqualTo("Begrunnelse2");

    }

    @Test
    void skal_lagre_og_oppdatere_vurderingresultat() {
        // Arrange
        var entityManager = getEntityManager();
        var scenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(ScenarioFarSøkerEngangsstønad.forAdopsjon());
        var behandling = scenario.lagre(repositoryProvider, klageRepository);
        entityManager.flush();

        var klageResultat = klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
        var builder1 = opprettVurderingResultat(klageResultat, KlageVurdertAv.NFP)
            .medBegrunnelse("Begrunnelse1");

        // Act
        klageRepository.lagreVurderingsResultat(behandling, builder1);
        entityManager.flush();
        entityManager.clear();

        var klageBehandlingId = behandling.getId();
        // Assert
        var klageVurderingResultat = klageRepository.hentKlageVurderingResultat(klageBehandlingId, KlageVurdertAv.NFP);
        assertThat(klageVurderingResultat).as("Formkrav NK opprettet").isPresent();
        assertThat(klageVurderingResultat.get().getBegrunnelse()).isEqualTo("Begrunnelse1");

        // Arrange
        var builder2 = opprettVurderingResultat(klageResultat, KlageVurdertAv.NFP)
            .medKlageVurdering(KlageVurdering.AVVIS_KLAGE)
            .medBegrunnelse("Begrunnelse2");

        // Act
        klageRepository.lagreVurderingsResultat(behandling, builder2);
        entityManager.flush();
        entityManager.clear();

        // Assert
        var klageVurderingResultat2 = klageRepository.hentKlageVurderingResultat(klageBehandlingId, KlageVurdertAv.NFP);
        assertThat(klageVurderingResultat2).as("Formkrav NK opprettet").isPresent();
        assertThat(klageVurderingResultat2.get().getBegrunnelse()).isEqualTo("Begrunnelse2");
    }

    @Test
    void skal_hente_formkrav_fra_ka_når_nfp_og_ka_har_vurdert() {
        // Pre Arrange
        var entityManager = getEntityManager();
        var scenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(ScenarioFarSøkerEngangsstønad.forAdopsjon());
        var behandling = scenario.lagre(repositoryProvider, klageRepository);
        entityManager.flush();

        // Arrange
        var klageResultat = klageRepository.hentEvtOpprettKlageResultat(behandling.getId());
        var builderNfp = opprettFormkravBuilder(klageResultat, KlageVurdertAv.NFP)
            .medBegrunnelse("Begrunnelse1");

        // Act
        klageRepository.lagreFormkrav(behandling,builderNfp);
        entityManager.flush();
        entityManager.clear();

        // Assert
        var klageFormkravNfp = klageRepository.hentGjeldendeKlageFormkrav(behandling.getId());
        assertThat(klageFormkravNfp).as("Formkrav Nfp opprettet").isPresent();
        assertThat(klageFormkravNfp.get().getKlageVurdertAv()).isEqualTo(KlageVurdertAv.NFP);

        // Arrange
        var builderKa = opprettFormkravBuilder(klageResultat, KlageVurdertAv.NK)
            .medBegrunnelse("Begrunnelse2");

        // Act
        klageRepository.lagreFormkrav(behandling,builderKa);
        entityManager.flush();
        entityManager.clear();

        // Assert
        var klageFormkravKa = klageRepository.hentGjeldendeKlageFormkrav(behandling.getId());
        assertThat(klageFormkravKa).as("Formkrav opprettet").isPresent();
        assertThat(klageFormkravKa.get().getKlageVurdertAv()).isEqualTo(KlageVurdertAv.NK);
    }

    private KlageVurderingResultat.Builder opprettVurderingResultat(KlageResultatEntitet klageResultat, KlageVurdertAv klageVurdertAv) {
        return KlageVurderingResultat.builder()
            .medKlageResultat(klageResultat)
            .medKlageVurdertAv(klageVurdertAv)
            .medKlageVurdering(KlageVurdering.MEDHOLD_I_KLAGE)
            .medKlageMedholdÅrsak(KlageMedholdÅrsak.ULIK_VURDERING);
    }

    private KlageFormkravEntitet.Builder opprettFormkravBuilder(KlageResultatEntitet klageResultat, KlageVurdertAv klageVurdertAv) {
        return KlageFormkravEntitet.builder()
            .medErKlagerPart(true)
            .medErFristOverholdt(true)
            .medErKonkret(true)
            .medErSignert(true)
            .medGjelderVedtak(true)
            .medMottattDato(LocalDate.now())
            .medBegrunnelse("Begrunnelse")
            .medKlageResultat(klageResultat)
            .medKlageVurdertAv(klageVurdertAv);
    }
}
