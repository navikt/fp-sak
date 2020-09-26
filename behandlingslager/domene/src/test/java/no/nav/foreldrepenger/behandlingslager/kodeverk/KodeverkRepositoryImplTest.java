package no.nav.foreldrepenger.behandlingslager.kodeverk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.foreldrepenger.behandlingslager.geografisk.Poststed;
import no.nav.foreldrepenger.behandlingslager.geografisk.PoststedKodeverkRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;

public class KodeverkRepositoryImplTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private PoststedKodeverkRepository repo = new PoststedKodeverkRepository(repoRule.getEntityManager());

    @Test
    public void test_hent_samme_verdi() {
        Poststed postSted1 = repo.finnPoststed("0103").orElse(null);
        Poststed postSted2 = repo.finnPoststed("0103").orElse(null);
        assertThat(postSted1).isEqualTo(postSted2);
    }

}
