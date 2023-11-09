package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;

class EndringerHarBegrunnelseValideringTest {

    @Test
    void okAlleHarBegrunnelse() {
        var opprinnelig = List.of(periode(null, PeriodeResultatType.MANUELL_BEHANDLING));
        var nyePerioder = List.of(periode("Ny begrunnelse", PeriodeResultatType.INNVILGET));
        var validering = new EndringerHarBegrunnelseValidering(opprinnelig);
        assertDoesNotThrow(() -> validering.utfør(nyePerioder));
    }

    @Test
    void feilVedTomBegrunnelse() {
        var opprinnelig = List.of(periode(null, PeriodeResultatType.MANUELL_BEHANDLING));
        var nyePerioder = List.of(periode("", PeriodeResultatType.INNVILGET));
        var validering = new EndringerHarBegrunnelseValidering(opprinnelig);
        assertThrows(TekniskException.class, () -> validering.utfør(nyePerioder));
    }

    @Test
    void feilVedNullBegrunnelse() {
        var opprinnelig = List.of(periode(null, PeriodeResultatType.MANUELL_BEHANDLING));
        var nyePerioder = List.of(periode(null, PeriodeResultatType.INNVILGET));
        var validering = new EndringerHarBegrunnelseValidering(opprinnelig);
        assertThrows(TekniskException.class, () -> validering.utfør(nyePerioder));
    }

    @Test
    void okÅMangleBegrunnelseHvisIngenEndring() {
        var opprinnelig = List.of(periode(null, PeriodeResultatType.INNVILGET));
        var nyePerioder = List.of(periode(null, PeriodeResultatType.INNVILGET));
        var validering = new EndringerHarBegrunnelseValidering(opprinnelig);
        assertDoesNotThrow(() -> validering.utfør(nyePerioder));
    }

    @Test
    void okÅMangleBegrunnelseHvisBareTrekkdagerEndring() {
        var opprinnelig = List.of(periode(new Trekkdager(10)));
        var nyePerioder = List.of(periode(new Trekkdager(15)));
        var validering = new EndringerHarBegrunnelseValidering(opprinnelig);
        assertDoesNotThrow(() -> validering.utfør(nyePerioder));
    }

    private ForeldrepengerUttakPeriode periode(Trekkdager trekkdager) {
        var periode = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medTrekkdager(trekkdager)
            .medArbeidsprosent(BigDecimal.TEN)
            .medUtbetalingsgrad(Utbetalingsgrad.TEN)
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, null, null))
            .build();
        var aktiviteter = List.of(periode);
        return new ForeldrepengerUttakPeriode.Builder()
            .medBegrunnelse(null)
            .medTidsperiode(new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(1)))
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medAktiviteter(aktiviteter)
            .build();
    }

    private ForeldrepengerUttakPeriode periode(String begrunnelse, PeriodeResultatType resultatType) {
        return new ForeldrepengerUttakPeriode.Builder()
            .medBegrunnelse(begrunnelse)
            .medResultatType(resultatType)
            .medTidsperiode(new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(1)))
            .build();
    }
}
