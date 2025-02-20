package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ExtendWith(JpaExtension.class)
class VergeGrunnlagEntitetTest extends EntityManagerAwareTest {
    private VergeRepository vergeRepository;
    private BehandlingRepository behandlingRepository;
    private EntityManager entityManager;

    @BeforeEach
    public void init(EntityManager entityManager) {
        this.entityManager = entityManager;
        var repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        vergeRepository = new VergeRepository(getEntityManager());
        behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Test
    void skal_lagre_verge_grunnlag() {
        var behandling = opprettBehandling();

        var bruker = NavBruker.opprettNyNB(AktørId.dummy());

        var vergeBuilder = new VergeEntitet.Builder()
                .medVergeType(VergeType.BARN)
                .medBruker(bruker);

        vergeRepository.lagreOgFlush(behandling.getId(), vergeBuilder);

        var resultat = vergeRepository.hentAggregat(behandling.getId());
        assertThat(resultat).isPresent();
    }

    private Behandling opprettBehandling() {
        var fagsak = opprettFagsak();
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
        return behandling;
    }

    private Fagsak opprettFagsak() {
        var bruker = NavBruker.opprettNyNB(AktørId.dummy());

        // Opprett fagsak
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, bruker, null, new Saksnummer("1000"));
        entityManager.persist(bruker);
        entityManager.persist(fagsak);
        entityManager.flush();
        return fagsak;
    }
}
