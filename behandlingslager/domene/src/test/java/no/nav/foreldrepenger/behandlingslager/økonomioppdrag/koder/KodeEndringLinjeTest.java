package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KodeEndringLinjeTest {

    @Test
    void fraKode_ok() {
        var endr = KodeEndringLinje.fraKode("ENDR");
        assertThat(endr).isEqualTo(KodeEndringLinje.ENDRING);
    }

    @Test
    void fraKode_nok() {
        Exception thrown = assertThrows(
            IllegalArgumentException.class,
            () -> KodeEndringLinje.fraKode("UKJENT_KODE")
        );

        assertTrue(thrown.getMessage().contains("Ukjent KodeEndringLinje"));
    }

    @Test
    void getNavn() {
        assertThat(KodeEndringLinje.NY.getNavn()).isNull();
    }

    @Test
    void getKode() {
        assertThat(KodeEndringLinje.ENDRING.getKode()).isEqualTo("ENDR");
    }

    @Test
    void getKodeverk() {
        assertThat(KodeEndringLinje.NY.getKodeverk()).isEqualTo("KODE_ENDRING_LINJE_TYPE");
    }

    @Test
    void equals() {
        assertEquals(KodeEndringLinje.NY, KodeEndringLinje.NY);
        assertEquals(KodeEndringLinje.fraKode("ENDR"), KodeEndringLinje.fraKode("ENDR"));
        assertNotEquals(KodeEndringLinje.NY, KodeEndringLinje.ENDRING);
        assertNotEquals(KodeEndringLinje.fraKode("ENDR"), KodeEndringLinje.fraKode("NY"));
    }
}
