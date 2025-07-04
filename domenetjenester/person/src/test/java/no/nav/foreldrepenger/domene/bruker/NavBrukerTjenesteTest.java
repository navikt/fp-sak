package no.nav.foreldrepenger.domene.bruker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoSpråk;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ExtendWith(MockitoExtension.class)
class NavBrukerTjenesteTest {

    @Mock
    private PersoninfoAdapter personinfoAdapter;
    private NavBrukerTjeneste brukerTjeneste;
    @Mock
    private NavBrukerRepository navBrukerRepository;

    @BeforeEach
    void oppsett() {
        brukerTjeneste = new NavBrukerTjeneste(navBrukerRepository, personinfoAdapter);
    }

    @Test
    void test_hent_forvent_tomt_svar() {
        var navBruker = brukerTjeneste.hentBrukerForAktørId(AktørId.dummy());
        assertThat(navBruker).isEmpty();
    }

    @Test
    void test_opprett_ny_bruker() {
        var aktør = AktørId.dummy();
        when(personinfoAdapter.hentForetrukketSpråk(aktør)).thenReturn(new PersoninfoSpråk(aktør, Språkkode.EN));

        var navBruker = brukerTjeneste.hentEllerOpprettFraAktørId(aktør);
        assertThat(navBruker.getId()).as("Forventer ny bruker som ikke er lagret returneres uten id.").isNull();

        assertThat(navBruker.getSpråkkode()).isEqualTo(Språkkode.EN);
    }

    @Test
    void test_hent_bruker() {
        var aktør = AktørId.dummy();
        when(personinfoAdapter.hentForetrukketSpråk(aktør)).thenReturn(new PersoninfoSpråk(aktør, Språkkode.NB));

        var navBruker = brukerTjeneste.hentEllerOpprettFraAktørId(aktør);

        assertThat(navBruker.getSpråkkode()).isEqualTo(Språkkode.NB);
        navBrukerRepository.lagre(navBruker);
        navBruker.setOpprettetTidspunkt(LocalDateTime.now().minusYears(1));
        navBrukerRepository.lagre(navBruker);

        when(personinfoAdapter.hentForetrukketSpråk(aktør)).thenReturn(new PersoninfoSpråk(aktør, Språkkode.NN));

        var navBrukerHent = brukerTjeneste.hentEllerOpprettFraAktørId(aktør);

        assertThat(navBrukerHent.getSpråkkode()).isEqualTo(Språkkode.NN);
    }
}
