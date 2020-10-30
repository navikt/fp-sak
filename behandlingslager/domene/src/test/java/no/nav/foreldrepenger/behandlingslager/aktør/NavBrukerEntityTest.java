package no.nav.foreldrepenger.behandlingslager.aktør;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class NavBrukerEntityTest extends EntityManagerAwareTest {

    @Test
    public void skal_lagre_og_hente_søker() {
        var entityManager = getEntityManager();
        var repository = new Repository(entityManager);
        var navBrukerRepo = new NavBrukerRepository(entityManager);

        AktørId aktørId = AktørId.dummy();
        NavBruker søker = NavBruker.opprettNyNB(aktørId);

        repository.lagre(søker);
        repository.flush();

        var bruker = navBrukerRepo.hent(aktørId);
        assertThat(bruker).isPresent();
        assertThat(bruker.get().getAktørId()).isEqualTo(aktørId);
    }
}
