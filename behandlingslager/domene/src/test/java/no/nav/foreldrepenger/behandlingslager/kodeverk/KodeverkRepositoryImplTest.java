package no.nav.foreldrepenger.behandlingslager.kodeverk;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.foreldrepenger.behandlingslager.geografisk.Poststed;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;

public class KodeverkRepositoryImplTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private KodeverkRepository repo = new KodeverkRepository(repoRule.getEntityManager());

    @Test
    public void test_hent_samme_verdi() {
        Poststed postSted1 = repo.finn(Poststed.class, "0103");
        Poststed postSted2 = repo.finn(Poststed.class, "0103");
        assertThat(postSted1).isEqualTo(postSted2);
    }

    @Test
    public void test_hent_flere_koder_samtidig_flere_ganger() {
        List<Poststed> poststeds = repo.finnListe(Poststed.class, asList("0103", "0104"));
        assertThat(poststeds).hasSize(2);
        List<Poststed> poststeds2 = repo.finnListe(Poststed.class, asList("0103", "0104"));
        assertThat(poststeds2).hasSize(2);
    }

}
