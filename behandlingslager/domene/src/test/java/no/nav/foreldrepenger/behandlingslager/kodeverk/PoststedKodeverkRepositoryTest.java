package no.nav.foreldrepenger.behandlingslager.kodeverk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.geografisk.Poststed;
import no.nav.foreldrepenger.behandlingslager.geografisk.PoststedKodeverkRepository;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class PoststedKodeverkRepositoryTest extends EntityManagerAwareTest {

    private PoststedKodeverkRepository repo;

    @BeforeEach
    void setUp() {
        repo = new PoststedKodeverkRepository(getEntityManager());
    }

    @Test
    public void test_hent_samme_verdi() {
        Poststed postSted1 = repo.finnPoststed("0103").orElse(null);
        Poststed postSted2 = repo.finnPoststed("0103").orElse(null);
        assertThat(postSted1).isEqualTo(postSted2);
    }

}
