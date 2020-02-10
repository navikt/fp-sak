package no.nav.foreldrepenger.skjæringstidspunkt.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;

import org.junit.Test;

public class RegisterinnhentingElastisiTest {

    private RegisterInnhentingIntervall innhentingIntervall = new RegisterInnhentingIntervall(Period.parse("P9M"), Period.parse("P6M"));

    @Test
    public void skal_gi_false_hvis_like() {
        LocalDate oppgitt = LocalDate.now();
        LocalDate bekreftet = LocalDate.now();

        boolean resultat = innhentingIntervall.erEndringIPerioden(oppgitt, bekreftet);
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_false_hvis_innenfor() {
        LocalDate oppgitt = LocalDate.now();
        LocalDate bekreftet = LocalDate.now().plusMonths(1);

        boolean resultat = innhentingIntervall.erEndringIPerioden(oppgitt, bekreftet);
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_gi_true_hvis_før() {
        LocalDate oppgitt = LocalDate.now();
        LocalDate bekreftet = LocalDate.now().minusYears(1);

        boolean resultat = innhentingIntervall.erEndringIPerioden(oppgitt, bekreftet);
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_gi_true_hvis_etter() {
        LocalDate oppgitt = LocalDate.now();
        LocalDate bekreftet = LocalDate.now().plusYears(1);

        boolean resultat = innhentingIntervall.erEndringIPerioden(oppgitt, bekreftet);
        assertThat(resultat).isTrue();
    }
}
