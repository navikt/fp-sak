package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HarSattUtbetalingsgradValideringTest {

    @Test
    void ok_når_utbetalingsgrad_er_satt_og_opprinnelig_periode_er_manuell() {
        var opprinnelig = perioder(PeriodeResultatType.MANUELL_BEHANDLING, null);
        var nye = perioder(PeriodeResultatType.INNVILGET, new Utbetalingsgrad(50));

        var validator = new HarSattUtbetalingsgradValidering(opprinnelig);
        assertDoesNotThrow(() -> validator.utfør(nye));
    }

    @Test
    void ok_når_utbetalingsgrad_er_satt_og_opprinnelig_periode_er_ikke_manuell() {
        var opprinnelig = perioder(PeriodeResultatType.INNVILGET, null);
        var nye = perioder(PeriodeResultatType.INNVILGET, null);

        var validator = new HarSattUtbetalingsgradValidering(opprinnelig);
        assertDoesNotThrow(() -> validator.utfør(nye));
    }

    @Test
    void ikke_ok_når_utbetalingsgrad_mangler_og_opprinnelig_periode_er_manuell() {
        var opprinnelig = perioder(PeriodeResultatType.MANUELL_BEHANDLING, null);
        var nye = perioder(PeriodeResultatType.INNVILGET, null);

        var validator = new HarSattUtbetalingsgradValidering(opprinnelig);
        assertThrows(TekniskException.class, () -> validator.utfør(nye));
    }

    private List<ForeldrepengerUttakPeriode> perioder(PeriodeResultatType resultat, Utbetalingsgrad utbetalingsgrad) {
        return List.of(periode(resultat, utbetalingsgrad));
    }

    private ForeldrepengerUttakPeriode periode(PeriodeResultatType resultatType, Utbetalingsgrad utbetalingsgrad) {
        var aktiviteter = List.of(
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
