package no.nav.foreldrepenger.domene.tid;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class DatoIntervallEntitetTest {

    @Test
    public void equalsTest() {
        DatoIntervallEntitet datoIntervallEntitet1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 10, 31));
        DatoIntervallEntitet datoIntervallEntitet2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 10, 31));

        assertEquals(true, datoIntervallEntitet1.equals(datoIntervallEntitet2));
    }

    @Test
    public void notEqualTest() {
        DatoIntervallEntitet datoIntervallEntitet1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 10, 31));
        DatoIntervallEntitet datoIntervallEntitet2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 10, 30));

        assertEquals(false, datoIntervallEntitet1.equals(datoIntervallEntitet2));
    }

}
