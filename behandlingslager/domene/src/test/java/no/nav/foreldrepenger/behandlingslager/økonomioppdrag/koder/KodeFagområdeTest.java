package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KodeFagområdeTest {

    @Test
    void fraKode_ok() {
        var endr = KodeFagområde.fraKode("REFUTG");
        assertThat(endr).isEqualTo(KodeFagområde.ENGANGSSTØNAD);
    }

    @Test
    void fraKode_nok() {
        Exception thrown = assertThrows(
            IllegalArgumentException.class,
            () -> KodeFagområde.fraKode("UKJENT_KODE")
        );

        assertTrue(thrown.getMessage().contains("Ukjent KodeFagområde"));
    }

    @Test
    void getNavn() {
        assertThat(KodeFagområde.FORELDREPENGER_AG.getNavn()).isNull();
    }

    @Test
    void getKode() {
        assertThat(KodeFagområde.SVANGERSKAPSPENGER_AG.getKode()).isEqualTo("SVPREF");
    }

    @Test
    void getKodeverk() {
        assertThat(KodeFagområde.FORELDREPENGER_BRUKER.getKodeverk()).isEqualTo("KODE_FAGOMRÅDE_TYPE");
    }

    @Test
    void equals() {
        assertEquals(KodeFagområde.FORELDREPENGER_BRUKER, KodeFagområde.FORELDREPENGER_BRUKER);
        assertEquals(KodeFagområde.fraKode("FPREF"), KodeFagområde.fraKode("FPREF"));
        assertNotEquals(KodeFagområde.ENGANGSSTØNAD, KodeFagområde.SVANGERSKAPSPENGER_BRUKER);
        assertNotEquals(KodeFagområde.fraKode("FPREF"), KodeFagområde.fraKode("SVPREF"));
    }
}
