package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KodeEndringTest {

    @Test
    void fraKode_ok() {
        var endr = KodeEndring.fraKode("ENDR");
        assertThat(endr).isEqualTo(KodeEndring.ENDRING);
    }

    @Test
    void fraKode_nok() {
        Exception thrown = assertThrows(
            IllegalArgumentException.class,
            () -> KodeEndring.fraKode("UKJENT_KODE")
        );

        assertTrue(thrown.getMessage().contains("Ukjent KodeEndring"));
    }

    @Test
    void getNavn() {
        assertThat(KodeEndring.NY.getNavn()).isNull();
    }

    @Test
    void getKode() {
        assertThat(KodeEndring.ENDRING.getKode()).isEqualTo("ENDR");
    }

    @Test
    void getKodeverk() {
        assertThat(KodeEndring.NY.getKodeverk()).isEqualTo("KODE_ENDRING_TYPE");
    }

    @Test
    void equals() {
        assertEquals(KodeEndring.NY, KodeEndring.NY);
        assertEquals(KodeEndring.fraKode("ENDR"), KodeEndring.fraKode("ENDR"));
        assertNotEquals(KodeEndring.NY, KodeEndring.UENDRET);
        assertNotEquals(KodeEndring.fraKode("ENDR"), KodeEndring.fraKode("UEND"));
    }
}
