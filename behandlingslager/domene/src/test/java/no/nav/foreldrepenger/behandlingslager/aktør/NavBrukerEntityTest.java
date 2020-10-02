package no.nav.foreldrepenger.behandlingslager.aktør;

import static java.time.Month.JANUARY;
import static no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn.KVINNE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class NavBrukerEntityTest extends EntityManagerAwareTest {
    
    @Test
    public void skal_lagre_og_hente_søker() {
        var entityManager = getEntityManager();
        var repository = new Repository(entityManager);
        var navBrukerRepo = new NavBrukerRepository(entityManager);

        AktørId aktørId = AktørId.dummy();
        NavBruker søker = NavBruker.opprettNy(
            new Personinfo.Builder()
                .medAktørId(aktørId)
                .medPersonIdent(new PersonIdent("12345678901"))
                .medNavn("Kari Nordmann")
                .medFødselsdato(LocalDate.of(1990, JANUARY, 1))
                .medNavBrukerKjønn(KVINNE)
                .medForetrukketSpråk(Språkkode.NB)
                .build());

        repository.lagre(søker);
        repository.flush();

        var bruker = navBrukerRepo.hent(aktørId);
        assertThat(bruker).isPresent();
        assertThat(bruker.get().getAktørId()).isEqualTo(aktørId);
    }
}
