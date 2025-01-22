package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class KodeKlassifikTest {

    @Test
    void fraKode_ok() {
        var kodeKlassifik = KodeKlassifik.fraKode("FPENFOD-OP");
        assertThat(kodeKlassifik).isEqualTo(KodeKlassifik.ES_FØDSEL);
    }

    @Test
    void fraKode_nok() {
        Exception thrown = assertThrows(
            IllegalArgumentException.class,
            () -> KodeKlassifik.fraKode("UKJENT_KODE")
        );

        assertThat(thrown.getMessage()).contains("Ukjent KodeKlassifik");
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
        assertThat(KodeKlassifik.FERIEPENGER_BRUKER).isEqualTo(KodeKlassifik.FERIEPENGER_BRUKER);
        assertThat(KodeKlassifik.fraKode("FPSVSNDFI")).isEqualTo(KodeKlassifik.fraKode("FPSVSNDFI"));
        assertThat(KodeKlassifik.SVP_FERIEPENGER_AG).isNotEqualTo(KodeKlassifik.FERIEPENGER_BRUKER);
        assertThat(KodeKlassifik.fraKode("FPATORD")).isNotEqualTo(KodeKlassifik.fraKode("FPSVSNDFI"));
    }
}
