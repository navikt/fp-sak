package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;

public class BareSplittetPerioderValideringTest {

    @Test
    public void enPeriodeKanSplittesIToHvisSammeFomOgTom() {
        LocalDate opprinneligFom = LocalDate.now();
        LocalDate opprinneligTom = LocalDate.now().plusWeeks(3);
        LocalDate førsteTom = opprinneligFom.plusWeeks(2);
        LocalDate sisteFom = førsteTom.plusDays(1);
        var opprinnelig = List.of(periodeMedDato(opprinneligFom, opprinneligTom));
        var nyePerioder = List.of(periodeMedDato(opprinneligFom, førsteTom), periodeMedDato(sisteFom, opprinneligTom));

        BareSplittetPerioderValidering validering = new BareSplittetPerioderValidering(opprinnelig);
        assertThatCode(() -> validering.utfør(nyePerioder)).doesNotThrowAnyException();
    }

    @Test
    public void enPeriodeKanIkkeSplittesIToHvisFørsteFomFørOpprinneligFom() {
        LocalDate opprinneligFom = LocalDate.now();
        LocalDate opprinneligTom = LocalDate.now().plusWeeks(3);
        LocalDate førsteFom = opprinneligFom.minusDays(1);
        LocalDate førsteTom = opprinneligFom.plusWeeks(2);
        LocalDate sisteFom = førsteTom.plusDays(1);
        var opprinnelig = List.of(periodeMedDato(opprinneligFom, opprinneligTom));
        var nyePerioder = List.of(periodeMedDato(førsteFom, førsteTom), periodeMedDato(sisteFom, opprinneligTom));

        BareSplittetPerioderValidering validering = new BareSplittetPerioderValidering(opprinnelig);
        assertThatThrownBy(() -> validering.utfør(nyePerioder)).isInstanceOf(TekniskException.class);
    }

    @Test
    public void enPeriodeKanIkkeSplittesIToHvisFørsteFomEtterOpprinneligFom() {
        LocalDate opprinneligFom = LocalDate.now();
        LocalDate opprinneligTom = LocalDate.now().plusWeeks(3);
        LocalDate førsteTom = opprinneligFom.plusWeeks(2);
        LocalDate førsteFom = opprinneligFom.plusDays(1);
        LocalDate sisteFom = førsteTom.plusDays(1);
        var opprinnelig = List.of(periodeMedDato(opprinneligFom, opprinneligTom));
        var nyePerioder = List.of(periodeMedDato(førsteFom, førsteTom), periodeMedDato(sisteFom, opprinneligTom));

        BareSplittetPerioderValidering validering = new BareSplittetPerioderValidering(opprinnelig);
        assertThatThrownBy(() -> validering.utfør(nyePerioder)).isInstanceOf(TekniskException.class);
    }

    @Test
    public void enPeriodeKanIkkeSplittesIToHvisSisteTomFørOpprinneligTom() {
        LocalDate opprinneligFom = LocalDate.now();
        LocalDate opprinneligTom = LocalDate.now().plusWeeks(3);
        LocalDate førsteTom = opprinneligFom.plusWeeks(2);
        LocalDate sisteFom = førsteTom.plusDays(1);
        LocalDate sisteTom = opprinneligTom.minusDays(1);
        var opprinnelig = List.of(periodeMedDato(opprinneligFom, opprinneligTom));
        var nyePerioder = List.of(periodeMedDato(opprinneligFom, førsteTom), periodeMedDato(sisteFom, sisteTom));

        BareSplittetPerioderValidering validering = new BareSplittetPerioderValidering(opprinnelig);
        assertThatThrownBy(() -> validering.utfør(nyePerioder)).isInstanceOf(TekniskException.class);
    }

    @Test
    public void enPeriodeKanSplittesITreHvisFomOgTomStemmerMedOpprinnelig() {
        LocalDate opprinneligFom = LocalDate.now();
        LocalDate opprinneligTom = LocalDate.now().plusWeeks(10);
        LocalDate førsteTom = opprinneligFom.plusWeeks(2);
        LocalDate andreFom = førsteTom.plusDays(1);
        LocalDate andreTom = andreFom.plusWeeks(2);
        LocalDate tredjeFom = andreTom.plusDays(1);
        var opprinnelig = List.of(periodeMedDato(opprinneligFom, opprinneligTom));
        var nyePerioder = List.of(periodeMedDato(opprinneligFom, førsteTom),
            periodeMedDato(andreFom, andreTom),
            periodeMedDato(tredjeFom, opprinneligTom));

        BareSplittetPerioderValidering validering = new BareSplittetPerioderValidering(opprinnelig);
        assertThatCode(() -> validering.utfør(nyePerioder)).doesNotThrowAnyException();
    }

    @Test
    public void enPeriodeKanIkkeSplittesIToLikePerioder() {
        LocalDate opprinneligFom = LocalDate.now();
        LocalDate opprinneligTom = LocalDate.now().plusWeeks(3);
        var opprinnelig = List.of(periodeMedDato(opprinneligFom, opprinneligTom));
        var nyePerioder = List.of(periodeMedDato(opprinneligFom, opprinneligTom), periodeMedDato(opprinneligFom, opprinneligTom));

        BareSplittetPerioderValidering validering = new BareSplittetPerioderValidering(opprinnelig);
        assertThatThrownBy(() -> validering.utfør(nyePerioder)).isInstanceOf(TekniskException.class);
    }

    @Test
    public void feilVedOverlappendePerioder() {
        LocalDate førsteFom = LocalDate.now();
        LocalDate førsteTom = LocalDate.now().plusDays(5);
        LocalDate andreTom = førsteTom.plusDays(10);
        var opprinnelig = List.of(periodeMedDato(førsteFom, andreTom));
        var nyePerioder = List.of(periodeMedDato(førsteFom, førsteFom), periodeMedDato(førsteTom, andreTom));

        BareSplittetPerioderValidering validering = new BareSplittetPerioderValidering(opprinnelig);
        assertThatThrownBy(() -> validering.utfør(nyePerioder)).isInstanceOf(TekniskException.class);
    }

    private ForeldrepengerUttakPeriodeAktivitet aktivitet() {
        return new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medArbeidsprosent(BigDecimal.TEN)
            .medUtbetalingsprosent(BigDecimal.ZERO)
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, null, null))
            .build();
    }

    private ForeldrepengerUttakPeriode periodeMedDato(LocalDate fom, LocalDate tom) {
        var aktivitet = aktivitet();
        return new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(new LocalDateInterval(fom, tom))
            .medAktiviteter(Collections.singletonList(aktivitet))
            .medResultatType(PeriodeResultatType.INNVILGET)
            .build();
    }
}
