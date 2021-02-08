package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TypeSatsTest {

    @Test
    void fraKode_ok() {
        var typeSats = TypeSats.fraKode("ENG");
        assertThat(typeSats).isEqualTo(TypeSats.ENGANG);
    }

    @Test
    void fraKode_nok_exception() {
        Exception thrown = assertThrows(
            IllegalArgumentException.class,
            () -> TypeSats.fraKode("UKJENT_KODE")
        );

        assertTrue(thrown.getMessage().contains("Ukjent TypeSats"));
    }

    @Test
    void getNavn() {
        assertThat(TypeSats.ENGANG.getNavn()).isNull();
    }

    @Test
    void getKode() {
        assertThat(TypeSats.DAGLIG.getKode()).isEqualTo("DAG");
    }

    @Test
    void getKodeverk() {
        assertThat(TypeSats.ENGANG.getKodeverk()).isEqualTo("TYPE_SATS_TYPE");
    }

    @Test
    void equals() {
        assertEquals(TypeSats.ENGANG, TypeSats.ENGANG);
        assertEquals(TypeSats.fraKode("ENG"), TypeSats.fraKode("ENG"));
        assertNotEquals(TypeSats.DAGLIG, TypeSats.ENGANG);
        assertNotEquals(TypeSats.fraKode("ENG"), TypeSats.fraKode("DAG"));
    }
}
