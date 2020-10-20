package no.nav.foreldrepenger.domene.iay.modell;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class AktivitetsAvtaleTest {

    @Test
    public void testPeriodeNotEqual() {
        AktivitetsAvtale avtale1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 8, 30))).build();

        AktivitetsAvtale avtale2 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 8, 31))).build();

        assertThat(avtale1.equals(avtale2)).isFalse();
    }

    @Test
    public void testPeriodeEqual() {
        AktivitetsAvtale avtale1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 8, 30))).build();

        AktivitetsAvtale avtale2 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 10, 1), LocalDate.of(2019, 8, 30))).build();

        assertThat(avtale1.equals(avtale2)).isTrue();
    }

}
