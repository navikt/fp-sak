package no.nav.foreldrepenger.behandlingslager.aktør;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

class NavBrukerEntityTest extends EntityManagerAwareTest {

    @Test
    void skal_lagre_og_hente_søker() {
        var entityManager = getEntityManager();
        var navBrukerRepo = new NavBrukerRepository(entityManager);

        var aktørId = AktørId.dummy();
        var søker = NavBruker.opprettNyNB(aktørId);

        entityManager.persist(søker);
        entityManager.flush();

        var bruker = navBrukerRepo.hent(aktørId);
        assertThat(bruker).isPresent();
        assertThat(bruker.get().getAktørId()).isEqualTo(aktørId);
    }
}
