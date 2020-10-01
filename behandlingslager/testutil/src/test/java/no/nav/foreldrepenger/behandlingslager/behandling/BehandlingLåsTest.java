package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class BehandlingLåsTest extends EntityManagerAwareTest {

    private BehandlingRepository behandlingRepository;
    private Repository repo;

    @BeforeEach
    public void setup() {
        repo = new Repository(getEntityManager());
        behandlingRepository = new BehandlingRepository(getEntityManager());
    }

    @Test
    public void skal_finne_behandling_gitt_id() {
        var fagsak = FagsakBuilder.nyEngangstønadForMor().medSaksnummer(new Saksnummer("2")).build();
        repo.lagre(fagsak.getNavBruker());
        repo.lagre(fagsak);
        repo.flush();
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repo.lagre(behandling);
        repo.flush();
        var lås = behandlingRepository.taSkriveLås(behandling);
        assertThat(lås).isNotNull();
        var resultat = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(resultat).isNotNull();
        behandlingRepository.lagre(resultat, lås);
    }

}
