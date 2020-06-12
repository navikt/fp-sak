package no.nav.foreldrepenger.domene.iay.modell;

import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.*;

public class AktivitetsAvtaleTest {

    @Test
    public void testPeriodeNotEqual() {
        AktivitetsAvtale avtale1 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 8, 30))).build();

        AktivitetsAvtale avtale2 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 8, 31))).build();

        assertEquals(false, avtale1.equals(avtale2));
    }

    @Test
    public void testPeriodeEqual() {
        AktivitetsAvtale avtale1 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 8, 30))).build();

        AktivitetsAvtale avtale2 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 8, 30))).build();

        assertEquals(true, avtale1.equals(avtale2));
    }

}
