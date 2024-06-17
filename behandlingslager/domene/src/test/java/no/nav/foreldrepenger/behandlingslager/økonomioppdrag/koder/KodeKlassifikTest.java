package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KodeKlassifikTest {

    @Test
    void fraKode_ok() {
        var kodeKlassifik = KodeKlassifik.fraKode("FPENFOD-OP");
        assertThat(kodeKlassifik).isEqualTo(KodeKlassifik.ES_FØDSEL);
    }

    @Test
    void fraKode_nok() {
        Exception thrown = assertThrows(IllegalArgumentException.class, () -> KodeKlassifik.fraKode("UKJENT_KODE"));

        assertTrue(thrown.getMessage().contains("Ukjent KodeKlassifik"));
    }

    @Test
    void getKode() {
        assertThat(KodeKlassifik.FPA_DAGPENGER.getKode()).isEqualTo("FPADATAL");
    }

    @Test
    void gjelderFerie() {
        assertThat(KodeKlassifik.FERIEPENGER_BRUKER.gjelderFeriepenger()).isTrue();
        assertThat(KodeKlassifik.FPA_FERIEPENGER_AG.gjelderFeriepenger()).isTrue();
        assertThat(KodeKlassifik.FPF_FERIEPENGER_AG.gjelderFeriepenger()).isTrue();
        assertThat(KodeKlassifik.SVP_FERIEPENGER_AG.gjelderFeriepenger()).isTrue();

        assertThat(KodeKlassifik.FPA_ARBEIDSTAKER.gjelderFeriepenger()).isFalse();
    }

    @Test
    void equals() {
        assertEquals(KodeKlassifik.FERIEPENGER_BRUKER, KodeKlassifik.FERIEPENGER_BRUKER);
        assertEquals(KodeKlassifik.fraKode("FPSVSNDFI"), KodeKlassifik.fraKode("FPSVSNDFI"));
        assertNotEquals(KodeKlassifik.SVP_FERIEPENGER_AG, KodeKlassifik.FERIEPENGER_BRUKER);
        assertNotEquals(KodeKlassifik.fraKode("FPATORD"), KodeKlassifik.fraKode("FPSVSNDFI"));
    }
}
