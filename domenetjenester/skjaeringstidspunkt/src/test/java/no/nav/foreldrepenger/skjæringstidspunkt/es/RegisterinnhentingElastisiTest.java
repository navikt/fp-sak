package no.nav.foreldrepenger.skjæringstidspunkt.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Test;

class RegisterinnhentingElastisiTest {

    private RegisterInnhentingIntervall innhentingIntervall = new RegisterInnhentingIntervall(Period.parse("P9M"), Period.parse("P6M"));

    @Test
    void skal_gi_false_hvis_like() {
        var oppgitt = LocalDate.now();
        var bekreftet = LocalDate.now();

        var resultat = innhentingIntervall.erEndringIPerioden(oppgitt, bekreftet);
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_false_hvis_innenfor() {
        var oppgitt = LocalDate.now();
        var bekreftet = LocalDate.now().plusMonths(1);

        var resultat = innhentingIntervall.erEndringIPerioden(oppgitt, bekreftet);
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_true_hvis_før() {
        var oppgitt = LocalDate.now();
        var bekreftet = LocalDate.now().minusYears(1);

        var resultat = innhentingIntervall.erEndringIPerioden(oppgitt, bekreftet);
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_true_hvis_etter() {
        var oppgitt = LocalDate.now();
        var bekreftet = LocalDate.now().plusYears(1);

        var resultat = innhentingIntervall.erEndringIPerioden(oppgitt, bekreftet);
        assertThat(resultat).isTrue();
    }
}
