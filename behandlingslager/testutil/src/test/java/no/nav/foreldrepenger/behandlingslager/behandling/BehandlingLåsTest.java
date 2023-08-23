package no.nav.foreldrepenger.behandlingslager.behandling;

import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BehandlingLåsTest extends EntityManagerAwareTest {

    private BehandlingRepository behandlingRepository;
    private EntityManager entityManager;

    @BeforeEach
    public void setup() {
        entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(getEntityManager());
    }

    @Test
    void skal_finne_behandling_gitt_id() {
        var fagsak = FagsakBuilder.nyEngangstønadForMor().medSaksnummer(new Saksnummer("299999")).build();
        entityManager.persist(fagsak.getNavBruker());
        entityManager.persist(fagsak);
        entityManager.flush();
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        entityManager.persist(behandling);
        entityManager.flush();
        var lås = behandlingRepository.taSkriveLås(behandling);
        assertThat(lås).isNotNull();
        var resultat = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(resultat).isNotNull();
        behandlingRepository.lagre(resultat, lås);
    }

}
