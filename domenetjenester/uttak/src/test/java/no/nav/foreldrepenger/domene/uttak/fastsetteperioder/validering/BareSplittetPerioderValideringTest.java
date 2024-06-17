package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;

class BareSplittetPerioderValideringTest {

    @Test
    void enPeriodeKanSplittesIToHvisSammeFomOgTom() {
        var opprinneligFom = LocalDate.now();
        var opprinneligTom = LocalDate.now().plusWeeks(3);
        var førsteTom = opprinneligFom.plusWeeks(2);
        var sisteFom = førsteTom.plusDays(1);
        var opprinnelig = List.of(periodeMedDato(opprinneligFom, opprinneligTom));
        var nyePerioder = List.of(periodeMedDato(opprinneligFom, førsteTom), periodeMedDato(sisteFom, opprinneligTom));

        var validering = new BareSplittetPerioderValidering(opprinnelig);
        assertDoesNotThrow(() -> validering.utfør(nyePerioder));
    }

    @Test
    void enPeriodeKanIkkeSplittesIToHvisFørsteFomFørOpprinneligFom() {
        var opprinneligFom = LocalDate.now();
        var opprinneligTom = LocalDate.now().plusWeeks(3);
        var førsteFom = opprinneligFom.minusDays(1);
        var førsteTom = opprinneligFom.plusWeeks(2);
        var sisteFom = førsteTom.plusDays(1);
        var opprinnelig = List.of(periodeMedDato(opprinneligFom, opprinneligTom));
        var nyePerioder = List.of(periodeMedDato(førsteFom, førsteTom), periodeMedDato(sisteFom, opprinneligTom));

        var validering = new BareSplittetPerioderValidering(opprinnelig);
        assertThrows(TekniskException.class, () -> validering.utfør(nyePerioder));
    }

    @Test
    void enPeriodeKanIkkeSplittesIToHvisFørsteFomEtterOpprinneligFom() {
        var opprinneligFom = LocalDate.now();
        var opprinneligTom = LocalDate.now().plusWeeks(3);
        var førsteTom = opprinneligFom.plusWeeks(2);
        var førsteFom = opprinneligFom.plusDays(1);
        var sisteFom = førsteTom.plusDays(1);
        var opprinnelig = List.of(periodeMedDato(opprinneligFom, opprinneligTom));
        var nyePerioder = List.of(periodeMedDato(førsteFom, førsteTom), periodeMedDato(sisteFom, opprinneligTom));

        var validering = new BareSplittetPerioderValidering(opprinnelig);
        assertThrows(TekniskException.class, () -> validering.utfør(nyePerioder));
    }

    @Test
    void enPeriodeKanIkkeSplittesIToHvisSisteTomFørOpprinneligTom() {
        var opprinneligFom = LocalDate.now();
        var opprinneligTom = LocalDate.now().plusWeeks(3);
        var førsteTom = opprinneligFom.plusWeeks(2);
        var sisteFom = førsteTom.plusDays(1);
        var sisteTom = opprinneligTom.minusDays(1);
        var opprinnelig = List.of(periodeMedDato(opprinneligFom, opprinneligTom));
        var nyePerioder = List.of(periodeMedDato(opprinneligFom, førsteTom), periodeMedDato(sisteFom, sisteTom));

        var validering = new BareSplittetPerioderValidering(opprinnelig);
        assertThrows(TekniskException.class, () -> validering.utfør(nyePerioder));
    }

    @Test
    void enPeriodeKanSplittesITreHvisFomOgTomStemmerMedOpprinnelig() {
        var opprinneligFom = LocalDate.now();
        var opprinneligTom = LocalDate.now().plusWeeks(10);
        var førsteTom = opprinneligFom.plusWeeks(2);
        var andreFom = førsteTom.plusDays(1);
        var andreTom = andreFom.plusWeeks(2);
        var tredjeFom = andreTom.plusDays(1);
        var opprinnelig = List.of(periodeMedDato(opprinneligFom, opprinneligTom));
        var nyePerioder = List.of(periodeMedDato(opprinneligFom, førsteTom), periodeMedDato(andreFom, andreTom),
            periodeMedDato(tredjeFom, opprinneligTom));

        var validering = new BareSplittetPerioderValidering(opprinnelig);
        assertDoesNotThrow(() -> validering.utfør(nyePerioder));
    }

    @Test
    void enPeriodeKanIkkeSplittesIToLikePerioder() {
        var opprinneligFom = LocalDate.now();
        var opprinneligTom = LocalDate.now().plusWeeks(3);
        var opprinnelig = List.of(periodeMedDato(opprinneligFom, opprinneligTom));
        var nyePerioder = List.of(periodeMedDato(opprinneligFom, opprinneligTom), periodeMedDato(opprinneligFom, opprinneligTom));

        var validering = new BareSplittetPerioderValidering(opprinnelig);
        assertThrows(TekniskException.class, () -> validering.utfør(nyePerioder));
    }

    @Test
    void feilVedOverlappendePerioder() {
        var førsteFom = LocalDate.now();
        var førsteTom = LocalDate.now().plusDays(5);
        var andreTom = førsteTom.plusDays(10);
        var opprinnelig = List.of(periodeMedDato(førsteFom, andreTom));
        var nyePerioder = List.of(periodeMedDato(førsteFom, førsteFom), periodeMedDato(førsteTom, andreTom));

        var validering = new BareSplittetPerioderValidering(opprinnelig);
        assertThrows(TekniskException.class, () -> validering.utfør(nyePerioder));
    }

    private ForeldrepengerUttakPeriodeAktivitet aktivitet() {
        return new ForeldrepengerUttakPeriodeAktivitet.Builder().medArbeidsprosent(BigDecimal.TEN)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, null, null))
            .build();
    }

    private ForeldrepengerUttakPeriode periodeMedDato(LocalDate fom, LocalDate tom) {
        var aktivitet = aktivitet();
        return new ForeldrepengerUttakPeriode.Builder().medTidsperiode(new LocalDateInterval(fom, tom))
            .medAktiviteter(Collections.singletonList(aktivitet))
            .medResultatType(PeriodeResultatType.INNVILGET)
            .build();
    }
}
