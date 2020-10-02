package no.nav.foreldrepenger.behandlingslager.geografisk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class LandkoderTest {

    @Test
    public void skal_sjekke_for_norge() {
        assertThat(Landkoder.erNorge("SWE")).isFalse();
        assertThat(Landkoder.erNorge("NOR")).isTrue();
    }
}
