package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class FastsettePerioderEndringTjenesteTest {

    private static final Behandling BEHANDLING = mock(Behandling.class);

    @Test
    void endretPeriodeSkalVæreEndring() {
        var opprinneligPerioder = new UttakResultatPerioderEntitet();
        var opprinneligPeriode = enkeltPeriodeAktivitet(new Trekkdager(2), Utbetalingsgrad.TEN);
        opprinneligPerioder.leggTilPeriode(opprinneligPeriode);
        var overstyrtPerioder = new UttakResultatPerioderEntitet();
        var overstyrtPeriode = enkeltPeriodeAktivitet(new Trekkdager(10), Utbetalingsgrad.ZERO);
        overstyrtPerioder.leggTilPeriode(overstyrtPeriode);
        var uttakResultat = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class)).medOpprinneligPerioder(opprinneligPerioder)
            .medOverstyrtPerioder(overstyrtPerioder)
            .build();
        var tjeneste = tjeneste(uttakResultat);

        var endringer = tjeneste.finnEndringerMellomOpprinneligOgOverstyrtForBehandling(BEHANDLING.getId());

        assertThat(endringer).hasSize(1);
        assertThat(endringer.get(0).getFom()).isEqualTo(opprinneligPeriode.getFom());
        assertThat(endringer.get(0).getTom()).isEqualTo(opprinneligPeriode.getTom());
        assertThat(endringer.get(0).getErEndret()).isTrue();
        assertThat(endringer.get(0).getErSlettet()).isFalse();
        assertThat(endringer.get(0).getErLagtTil()).isFalse();
    }

    @Test
    void splittingAvPeriodeSkalGiEnSlettingOgToLagtTilEndringer() {
        var opprinneligPerioder = new UttakResultatPerioderEntitet();
        var opprinneligPeriode = minimumPeriode().build();
        opprinneligPerioder.leggTilPeriode(opprinneligPeriode);
        var overstyrtPerioder = new UttakResultatPerioderEntitet();
        var overstyrtPeriode1 = new UttakResultatPeriodeEntitet.Builder(LocalDate.now().minusMonths(1),
            LocalDate.now().minusWeeks(2)).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
        var overstyrtPeriode2 = new UttakResultatPeriodeEntitet.Builder(LocalDate.now().minusWeeks(2).plusDays(1), LocalDate.now()).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
        overstyrtPerioder.leggTilPeriode(overstyrtPeriode1);
        overstyrtPerioder.leggTilPeriode(overstyrtPeriode2);
        var uttakResultat = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class)).medOpprinneligPerioder(opprinneligPerioder)
            .medOverstyrtPerioder(overstyrtPerioder)
            .build();
        var tjeneste = tjeneste(uttakResultat);

        var endringer = tjeneste.finnEndringerMellomOpprinneligOgOverstyrtForBehandling(BEHANDLING.getId());

        assertThat(endringer).hasSize(3);
        assertThat(endringer.get(0).getFom()).isEqualTo(opprinneligPeriode.getFom());
        assertThat(endringer.get(0).getTom()).isEqualTo(opprinneligPeriode.getTom());
        assertThat(endringer.get(0).getErEndret()).isFalse();
        assertThat(endringer.get(0).getErSlettet()).isTrue();
        assertThat(endringer.get(0).getErLagtTil()).isFalse();
        assertThat(endringer.get(1).getFom()).isEqualTo(overstyrtPeriode1.getFom());
        assertThat(endringer.get(1).getTom()).isEqualTo(overstyrtPeriode1.getTom());
        assertThat(endringer.get(1).getErEndret()).isFalse();
        assertThat(endringer.get(1).getErSlettet()).isFalse();
        assertThat(endringer.get(1).getErLagtTil()).isTrue();
        assertThat(endringer.get(2).getFom()).isEqualTo(overstyrtPeriode2.getFom());
        assertThat(endringer.get(2).getTom()).isEqualTo(overstyrtPeriode2.getTom());
        assertThat(endringer.get(2).getErEndret()).isFalse();
        assertThat(endringer.get(2).getErSlettet()).isFalse();
        assertThat(endringer.get(2).getErLagtTil()).isTrue();
    }

    @Test
    void skalReturnereTomListeHvisOverstyrtErNull() {
        var opprinneligPerioder = new UttakResultatPerioderEntitet();
        var opprinneligPeriode = enkeltPeriodeAktivitet(new Trekkdager(2), Utbetalingsgrad.ZERO);
        opprinneligPerioder.leggTilPeriode(opprinneligPeriode);
        var uttakResultat = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class)).medOpprinneligPerioder(opprinneligPerioder)
            .medOverstyrtPerioder(null)
            .build();
        var tjeneste = tjeneste(uttakResultat);

        var endringer = tjeneste.finnEndringerMellomOpprinneligOgOverstyrtForBehandling(BEHANDLING.getId());

        assertThat(endringer).isEmpty();
    }

    @Test
    void endretPeriodeSkalVæreEndringFlereAktiviteter() {
        var opprinneligPerioder = new UttakResultatPerioderEntitet();

        var opprinneligPeriode = minimumPeriode().build();
        var opprinneligAktivitet1 = periodeAktivitet(opprinneligPeriode, new Trekkdager(1));
        var opprinneligAktivitet2 = periodeAktivitet(opprinneligPeriode, new Trekkdager(1));
        var opprinneligAktivitet3 = periodeAktivitet(opprinneligPeriode, new Trekkdager(2));
        opprinneligPeriode.leggTilAktivitet(opprinneligAktivitet1);
        opprinneligPeriode.leggTilAktivitet(opprinneligAktivitet2);
        opprinneligPeriode.leggTilAktivitet(opprinneligAktivitet3);

        opprinneligPerioder.leggTilPeriode(opprinneligPeriode);
        var overstyrtPerioder = new UttakResultatPerioderEntitet();
        var overstyrtPeriode = minimumPeriode().build();
        overstyrtPeriode.leggTilAktivitet(opprinneligAktivitet1);
        overstyrtPeriode.leggTilAktivitet(opprinneligAktivitet3);
        overstyrtPerioder.leggTilPeriode(overstyrtPeriode);
        var uttakResultat = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class)).medOpprinneligPerioder(opprinneligPerioder)
            .medOverstyrtPerioder(overstyrtPerioder)
            .build();
        var tjeneste = tjeneste(uttakResultat);

        var endringer = tjeneste.finnEndringerMellomOpprinneligOgOverstyrtForBehandling(BEHANDLING.getId());

        assertThat(endringer).hasSize(1);
        assertThat(endringer.get(0).getFom()).isEqualTo(opprinneligPeriode.getFom());
        assertThat(endringer.get(0).getTom()).isEqualTo(opprinneligPeriode.getTom());
        assertThat(endringer.get(0).getErEndret()).isTrue();
        assertThat(endringer.get(0).getErSlettet()).isFalse();
        assertThat(endringer.get(0).getErLagtTil()).isFalse();
    }

    private UttakResultatPeriodeAktivitetEntitet periodeAktivitet(UttakResultatPeriodeEntitet periode, Trekkdager trekkdager) {
        var uttakAktivitet = uttakAktivitet();
        return new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet).medArbeidsprosent(BigDecimal.ZERO)
            .medTrekkdager(trekkdager)
            .build();
    }

    private UttakResultatPeriodeEntitet enkeltPeriodeAktivitet(Trekkdager trekkdager, Utbetalingsgrad utbetalingsgrad) {
        var periodeBuilder = minimumPeriode();
        var periode = periodeBuilder.build();
        var uttakAktivitet = uttakAktivitet();
        var periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet).medArbeidsprosent(BigDecimal.ZERO)
            .medTrekkdager(trekkdager)
            .medUtbetalingsgrad(utbetalingsgrad)
            .build();
        periode.leggTilAktivitet(periodeAktivitet);
        return periode;
    }

    private UttakAktivitetEntitet uttakAktivitet() {
        return new UttakAktivitetEntitet.Builder().medArbeidsforhold(Arbeidsgiver.virksomhet("orgnr"), InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
    }

    private UttakResultatPeriodeEntitet.Builder minimumPeriode() {
        return new UttakResultatPeriodeEntitet.Builder(LocalDate.now().minusMonths(1), LocalDate.now()).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.UKJENT);
    }

    private FastsettePerioderEndringTjeneste tjeneste(UttakResultatEntitet uttakResultat) {
        return new FastsettePerioderEndringTjeneste(uttakRepository(uttakResultat));
    }

    private FpUttakRepository uttakRepository(UttakResultatEntitet uttakResultat) {
        var mock = mock(FpUttakRepository.class);
        when(mock.hentUttakResultat(Mockito.any())).thenReturn(uttakResultat);
        return mock;
    }

}
