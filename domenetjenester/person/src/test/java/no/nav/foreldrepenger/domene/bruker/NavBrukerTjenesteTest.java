package no.nav.foreldrepenger.domene.bruker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoSpråk;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ExtendWith(MockitoExtension.class)
public class NavBrukerTjenesteTest extends EntityManagerAwareTest {

    @Mock
    private PersoninfoAdapter personinfoAdapter;
    private NavBrukerTjeneste brukerTjeneste;
    private NavBrukerRepository navBrukerRepository;

    @BeforeEach
    public void oppsett() {
        navBrukerRepository = new NavBrukerRepository(getEntityManager());
        brukerTjeneste = new NavBrukerTjeneste(navBrukerRepository, personinfoAdapter);
    }

    @Test
    public void test_hent_forvent_tomt_svar() {
        Optional<NavBruker> navBruker = brukerTjeneste.hentBrukerForAktørId(AktørId.dummy());
        assertThat(navBruker).isEmpty();
    }

    @Test
    public void test_opprett_ny_bruker() {
        var aktør = AktørId.dummy();
        when(personinfoAdapter.hentForetrukketSpråk(aktør)).thenReturn(new PersoninfoSpråk(aktør, Språkkode.EN));

        NavBruker navBruker = brukerTjeneste.hentEllerOpprettFraAktørId(aktør);
        assertThat(navBruker.getId()).as("Forventer ny bruker som ikke er lagret returneres uten id.").isNull();

        navBrukerRepository.lagre(navBruker);
        var hentet = navBrukerRepository.hent(aktør);

        assertThat(hentet).isPresent();
        assertThat(hentet.map(NavBruker::getSpråkkode).orElse(Språkkode.UDEFINERT)).isEqualTo(Språkkode.EN);
    }

    @Test
    public void test_hent_bruker() {
        var aktør = AktørId.dummy();
        when(personinfoAdapter.hentForetrukketSpråk(aktør)).thenReturn(new PersoninfoSpråk(aktør, Språkkode.NB));

        NavBruker navBruker = brukerTjeneste.hentEllerOpprettFraAktørId(aktør);

        assertThat(navBruker.getSpråkkode()).isEqualTo(Språkkode.NB);
        navBrukerRepository.lagre(navBruker);
        navBruker.setOpprettetTidspunkt(LocalDateTime.now().minusYears(1));
        // Whitebox.setInternalState(navBruker, "opprettetTidspunkt",
        // LocalDateTime.now().minusYears(1));
        navBrukerRepository.lagre(navBruker);

        when(personinfoAdapter.hentForetrukketSpråk(aktør)).thenReturn(new PersoninfoSpråk(aktør, Språkkode.NN));

        NavBruker navBrukerHent = brukerTjeneste.hentEllerOpprettFraAktørId(aktør);

        assertThat(navBrukerHent.getSpråkkode()).isEqualTo(Språkkode.NN);
    }
}
