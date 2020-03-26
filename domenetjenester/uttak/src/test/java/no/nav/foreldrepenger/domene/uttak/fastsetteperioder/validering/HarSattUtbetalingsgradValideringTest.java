package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;

public class HarSattUtbetalingsgradValideringTest {

    @Test
    public void ok_når_utbetalingsgrad_er_satt_og_opprinnelig_periode_er_manuell() {
        var opprinnelig = perioder(PeriodeResultatType.MANUELL_BEHANDLING, null);
        var nye = perioder(PeriodeResultatType.INNVILGET, BigDecimal.valueOf(50));

        HarSattUtbetalingsgradValidering validator = new HarSattUtbetalingsgradValidering(opprinnelig);
        assertThatCode(() -> validator.utfør(nye)).doesNotThrowAnyException();
    }

    @Test
    public void ok_når_utbetalingsgrad_er_satt_og_opprinnelig_periode_er_ikke_manuell() {
        var opprinnelig = perioder(PeriodeResultatType.INNVILGET, null);
        var nye = perioder(PeriodeResultatType.INNVILGET, null);

        HarSattUtbetalingsgradValidering validator = new HarSattUtbetalingsgradValidering(opprinnelig);
        assertThatCode(() -> validator.utfør(nye)).doesNotThrowAnyException();
    }

    @Test
    public void ikke_ok_når_utbetalingsgrad_mangler_og_opprinnelig_periode_er_manuell() {
        var opprinnelig = perioder(PeriodeResultatType.MANUELL_BEHANDLING, null);
        var nye = perioder(PeriodeResultatType.INNVILGET, null);

        HarSattUtbetalingsgradValidering validator = new HarSattUtbetalingsgradValidering(opprinnelig);
        assertThatCode(() -> validator.utfør(nye)).isInstanceOf(TekniskException.class);
    }

    private List<ForeldrepengerUttakPeriode> perioder(PeriodeResultatType resultat, BigDecimal utbetalingsgrad) {
        return List.of(periode(resultat, utbetalingsgrad));
    }

    private ForeldrepengerUttakPeriode periode(PeriodeResultatType resultatType, BigDecimal utbetalingsgrad) {
        List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter = List.of(
            new ForeldrepengerUttakPeriodeAktivitet.Builder()
                .medArbeidsprosent(BigDecimal.ZERO)
                .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, null, null))
                .medUtbetalingsgrad(utbetalingsgrad).build()
        );
        return new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(1)))
            .medAktiviteter(aktiviteter)
            .medResultatType(resultatType)
            .build();
    }
}
