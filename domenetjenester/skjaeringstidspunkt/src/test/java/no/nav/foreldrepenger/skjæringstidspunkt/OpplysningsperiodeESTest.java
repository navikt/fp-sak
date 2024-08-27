package no.nav.foreldrepenger.skjæringstidspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class OpplysningsperiodeESTest {

    @Test
    void skal_gi_false_hvis_like() {
        var oppgitt = LocalDate.now();
        var bekreftet = LocalDate.now();

        var resultat = OpplysningsPeriodeTjeneste.erEndringIPerioden(oppgitt, bekreftet);
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_false_hvis_innenfor() {
        var oppgitt = LocalDate.now();
        var bekreftet = LocalDate.now().plusMonths(1);

        var resultat = OpplysningsPeriodeTjeneste.erEndringIPerioden(oppgitt, bekreftet);
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_gi_true_hvis_før() {
        var oppgitt = LocalDate.now();
        var bekreftet = LocalDate.now().minusYears(1);

        var resultat = OpplysningsPeriodeTjeneste.erEndringIPerioden(oppgitt, bekreftet);
        assertThat(resultat).isTrue();
    }

    @Test
    void skal_gi_true_hvis_etter() {
        var oppgitt = LocalDate.now();
        var bekreftet = LocalDate.now().plusYears(1);

        var resultat = OpplysningsPeriodeTjeneste.erEndringIPerioden(oppgitt, bekreftet);
        assertThat(resultat).isTrue();
    }
}
