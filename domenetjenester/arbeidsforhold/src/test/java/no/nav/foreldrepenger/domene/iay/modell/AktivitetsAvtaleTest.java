package no.nav.foreldrepenger.domene.iay.modell;

import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AktivitetsAvtaleTest {

    @Test
    void testPeriodeNotEqual() {
        var avtale1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 8, 30))).build();

        var avtale2 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 8, 31))).build();

        assertThat(avtale1.equals(avtale2)).isFalse();
    }

    @Test
    void testPeriodeEqual() {
        var avtale1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 8, 30))).build();

        var avtale2 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 8, 30))).build();

        assertThat(avtale1.equals(avtale2)).isTrue();
    }

}
