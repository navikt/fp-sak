package no.nav.foreldrepenger.domene.tid;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class DatoIntervallEntitetTest {

    @Test
    public void equalsTest() {
        var datoIntervallEntitet1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 10, 31));
        var datoIntervallEntitet2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 10, 31));

        assertThat(datoIntervallEntitet1.equals(datoIntervallEntitet2)).isTrue();
    }

    @Test
    public void notEqualTest() {
        var datoIntervallEntitet1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 10, 31));
        var datoIntervallEntitet2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 10, 30));

        assertThat(datoIntervallEntitet1.equals(datoIntervallEntitet2)).isFalse();
    }

}
