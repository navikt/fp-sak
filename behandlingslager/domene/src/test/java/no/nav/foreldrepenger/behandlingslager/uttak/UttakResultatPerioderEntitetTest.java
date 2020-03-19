package no.nav.foreldrepenger.behandlingslager.uttak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class UttakResultatPerioderEntitetTest {

    private LocalDate start;
    private UttakResultatPerioderEntitet perioder;

    @Before
    public void oppsett() {
        start = LocalDate.now();
        perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(lagPeriode(start, start.plusWeeks(6).minusDays(1)));
        perioder.leggTilPeriode(lagPeriode(start.plusWeeks(6), start.plusWeeks(16).minusDays(1)));
    }

    @Test
    public void skal_finne_første_uttaksdato_blandt_perioder() {
        assertThat(perioder.finnFørsteUttaksdato().get()).isEqualTo(start);
    }

    @Test
    public void skal_finne_siste_uttaksdato_blandt_perioder() {
        assertThat(perioder.finnSisteUttaksdato().get()).isEqualTo(start.plusWeeks(16).minusDays(1));
    }

    @Test
    public void skal_takle_at_det_er_ingen_perioder_når_første_og_siste_periode_skal_finnes() {
        perioder = new UttakResultatPerioderEntitet();

        assertThat(perioder.finnFørsteUttaksdato()).isEqualTo(Optional.empty());
        assertThat(perioder.finnSisteUttaksdato()).isEqualTo(Optional.empty());
    }

    @Test
    public void periode_skal_sorteres_når_de_hentes_ut() {
        perioder = new UttakResultatPerioderEntitet();
        //Legger til periodene i feil rekkefølge
        perioder.leggTilPeriode(lagPeriode(start.plusWeeks(2), start.plusWeeks(3).minusDays(1)));
        perioder.leggTilPeriode(lagPeriode(start.plusWeeks(1), start.plusWeeks(2).minusDays(1)));
        perioder.leggTilPeriode(lagPeriode(start, start.plusWeeks(1).minusDays(1)));

        var sortertePerioder = perioder.getPerioder();
        assertThat(sortertePerioder).hasSize(3);
        assertThat(sortertePerioder.get(0).getFom()).isEqualTo(start);
        assertThat(sortertePerioder.get(0).getTom()).isEqualTo(start.plusWeeks(1).minusDays(1));
        assertThat(sortertePerioder.get(1).getFom()).isEqualTo(start.plusWeeks(1));
        assertThat(sortertePerioder.get(1).getTom()).isEqualTo(start.plusWeeks(2).minusDays(1));
        assertThat(sortertePerioder.get(2).getFom()).isEqualTo(start.plusWeeks(2));
        assertThat(sortertePerioder.get(2).getTom()).isEqualTo(start.plusWeeks(3).minusDays(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void skal_feile_dersom_det_blir_lagt_til_overlappende_perioder() {
        start = LocalDate.now();
        perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(lagPeriode(start, start.plusWeeks(6)));
        perioder.leggTilPeriode(lagPeriode(start.plusWeeks(6), start.plusWeeks(16).minusDays(1)));
    }

    private UttakResultatPeriodeEntitet lagPeriode(LocalDate fom, LocalDate tom) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom).medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
    }

}
