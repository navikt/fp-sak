package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KodeStatusLinjeTest {

    @Test
    void fraKode_ok() {
        var endr = KodeStatusLinje.fraKode("OPPH");
        assertThat(endr).isEqualTo(KodeStatusLinje.OPPHØR);
    }

    @Test
    void fraKode_nok() {
        Exception thrown = assertThrows(
            IllegalArgumentException.class,
            () -> KodeStatusLinje.fraKode("UKJENT_KODE")
        );

        assertTrue(thrown.getMessage().contains("Ukjent KodeStatusLinje"));
    }

    @Test
    void getNavn() {
        assertThat(KodeStatusLinje.OPPHØR.getNavn()).isNull();
    }

    @Test
    void getKode() {
        assertThat(KodeStatusLinje.OPPHØR.getKode()).isEqualTo("OPPH");
    }

    @Test
    void getKodeverk() {
        assertThat(KodeStatusLinje.OPPHØR.getKodeverk()).isEqualTo("KODE_STATUS_LINJE_TYPE");
    }

    @Test
    void equals() {
        assertEquals(KodeStatusLinje.OPPHØR, KodeStatusLinje.OPPHØR);
        assertEquals(KodeStatusLinje.fraKode("OPPH"), KodeStatusLinje.fraKode("OPPH"));
    }

}
