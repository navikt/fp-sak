package no.nav.foreldrepenger.behandlingslager.hendelser.mottak;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.hendelser.HendelsemottakRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;

public class HendelsemottakRepositoryImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private HendelsemottakRepository repo = new HendelsemottakRepository(repoRule.getEntityManager());

    @Test
    public void skal_si_at_hendeles_er_ny_når_den_ikke_er_registret() throws Exception {
        assertThat(repo.hendelseErNy("erstatter")).isTrue();
    }

    @Test
    public void skal_lagre_hendelse_og_sjekke_om_finnes() throws Exception {
        repo.registrerMottattHendelse("erstatter");
        assertThat(repo.hendelseErNy("erstatter")).isFalse();
    }
}
