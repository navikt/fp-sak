package no.nav.foreldrepenger.behandlingslager.aktør;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class BrukerTjenesteTest extends EntityManagerAwareTest {

    private BrukerTjeneste brukerTjeneste;

    @BeforeEach
    public void oppsett() {
        brukerTjeneste = new BrukerTjeneste(new NavBrukerRepository(getEntityManager()));
    }


    @Test
    public void test_opprett_ny_bruker() {
        NavBruker navBruker = brukerTjeneste.hentEllerOpprettFraAktorId(AktørId.dummy(), Språkkode.NB);
        assertThat(navBruker.getId()).as("Forventer at nyTerminbekreftelse bruker som ikke er lagret returneres uten id.").isNull();
    }

    @Test
    public void test_hent_bruker() {
        var aktør = AktørId.dummy();

        NavBruker navBruker = brukerTjeneste.hentEllerOpprettFraAktorId(aktør, Språkkode.NN);
        assertThat(navBruker.getId()).as("Forventer at nyTerminbekreftelse bruker som ikke er lagret returneres uten id.").isNull();

        NavBruker navBrukerHent = brukerTjeneste.hentEllerOpprettFraAktorId(aktør, Språkkode.NN);
        assertThat(navBrukerHent.getId()).as("Forventer at vi henter opp eksisterende bruker.").isEqualTo(navBruker.getId());
    }
}
