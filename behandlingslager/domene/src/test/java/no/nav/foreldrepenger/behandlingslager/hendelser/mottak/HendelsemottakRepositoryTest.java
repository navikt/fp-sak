package no.nav.foreldrepenger.behandlingslager.hendelser.mottak;

import no.nav.foreldrepenger.behandlingslager.hendelser.HendelsemottakRepository;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HendelsemottakRepositoryTest extends EntityManagerAwareTest {

    @Test
    void skal_si_at_hendeles_er_ny_når_den_ikke_er_registret() {
        var repo = new HendelsemottakRepository(getEntityManager());
        assertThat(repo.hendelseErNy("erstatter")).isTrue();
    }

    @Test
    void skal_lagre_hendelse_og_sjekke_om_finnes() {
        var repo = new HendelsemottakRepository(getEntityManager());
        repo.registrerMottattHendelse("erstatter");
        assertThat(repo.hendelseErNy("erstatter")).isFalse();
    }
}
