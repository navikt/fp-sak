package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

public class NyKlasseUtenTest {


    public NyKlasseUtenTest() {
    }

    public static void metode(Long tall) {
        if (tall == null) {
            throw new IllegalArgumentException("tall kan ikke være null");
        }
        if (tall < 0) {
            throw new IllegalArgumentException("tall kan ikke være negativ");
        }

        if (tall > 100) {
            throw new IllegalArgumentException("tall kan ikke være større enn 100");
        }
        tall += 1;

    }
}
