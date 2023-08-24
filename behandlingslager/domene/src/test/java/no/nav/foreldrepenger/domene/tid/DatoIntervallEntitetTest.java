package no.nav.foreldrepenger.domene.tid;


import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DatoIntervallEntitetTest {

    @Test
    void equalsTest() {
        var datoIntervallEntitet1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 10, 31));
        var datoIntervallEntitet2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 10, 31));

        assertThat(datoIntervallEntitet1.equals(datoIntervallEntitet2)).isTrue();
    }

    @Test
    void notEqualTest() {
        var datoIntervallEntitet1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 10, 31));
        var datoIntervallEntitet2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 10, 30));

        assertThat(datoIntervallEntitet1.equals(datoIntervallEntitet2)).isFalse();
    }

}
