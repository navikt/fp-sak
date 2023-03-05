package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;


class BehandlingKandidaterRepositoryTest extends EntityManagerAwareTest {


    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        behandlingKandidaterRepository = new BehandlingKandidaterRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    void skal_finne_en_kandidat_for_automatisk_gjenopptagelse() {
        // Arrange
        var behandling = new BasicBehandlingBuilder(getEntityManager()).opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT);
        AksjonspunktTestSupport.setFrist(aksjonspunkt, LocalDateTime.now().minusMinutes(1), Venteårsak.FOR_TIDLIG_SOKNAD);
        behandlingRepository.lagre(behandling);

        // Act
        var behandlinger = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();

        // Assert
        assertThat(behandlinger).contains(behandling);

    }
}
