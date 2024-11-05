package no.nav.foreldrepenger.mottak.hendelser.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.ToForeldreBarnDødTjeneste;

@ExtendWith(MockitoExtension.class)
class ToForeldreBarnDødTjenesteTest {

    @Mock
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    private ToForeldreBarnDødTjeneste toForeldreBarnDødTjeneste;

    private Behandling behandlingF1;
    private Behandling behandlingF2;

    @BeforeEach
    public void oppsett() {
        var scenarioF1 = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenarioF1.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        var scenarioF2 = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenarioF2.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        behandlingF1 = scenarioF1.lagMocked();
        behandlingF2 = scenarioF2.lagMocked();
    }

    @Test
    void skal_velge_behandling_f1_når_f1_har_aktivt_uttak() {
        // Arrange Foreldre1 (F1) har aktivt uttak nå, og velges automatisk
        var uttakF1 = new ForeldrepengerUttak(lagPerioderMedFullUtbetaling(LocalDate.now().minusDays(15)));
        var uttakF2 = new ForeldrepengerUttak(lagPerioderMedFullUtbetaling(LocalDate.now()));

        when(uttakTjeneste.hentHvisEksisterer(Mockito.eq(behandlingF1.getId()))).thenReturn(Optional.of(uttakF1));

        toForeldreBarnDødTjeneste = new ToForeldreBarnDødTjeneste(uttakTjeneste);

        // Act
        var behandlingSomSkalRevurderes = toForeldreBarnDødTjeneste.finnBehandlingSomSkalRevurderes(behandlingF1, behandlingF2);

        // Assert
        assertThat(behandlingSomSkalRevurderes).isEqualTo(behandlingF1);
    }

    @Test
    void skal_velge_behandling_f1_når_f1_har_nærmeste_uttak() {
        // Arrange Foreldre1 (F1) har nærmeste uttak fremover i tid.
        // Nærmeste dato for F1 er 11 dager frem i tid;
        // Nærmeste dato for F2 er 7 dager tilbake i tid, men pga bufferet trekker fra
        // 14 slik at denne avstanden blir regnet som 21 dager
        // Nærmeste dato F2 blir dermed frem i tid: 14 dager.
        var uttakF1 = new ForeldrepengerUttak(lagPerioderMedFullUtbetaling(LocalDate.now().minusDays(3)));
        var uttakF2 = new ForeldrepengerUttak(lagPerioderMedFullUtbetaling(LocalDate.now()));

        when(uttakTjeneste.hentHvisEksisterer(Mockito.eq(behandlingF1.getId()))).thenReturn(Optional.of(uttakF1));
        when(uttakTjeneste.hentHvisEksisterer(Mockito.eq(behandlingF2.getId()))).thenReturn(Optional.of(uttakF2));

        toForeldreBarnDødTjeneste = new ToForeldreBarnDødTjeneste(uttakTjeneste);

        // Act
        var behandlingSomSkalRevurderes = toForeldreBarnDødTjeneste.finnBehandlingSomSkalRevurderes(behandlingF1, behandlingF2);

        // Assert
        assertThat(behandlingSomSkalRevurderes).isEqualTo(behandlingF1);
    }

    @Test
    void skal_velge_behandling_f2_når_f1_har_nærmeste_uttak_men_ingen_utbetaling() {
        // Arrange Foreldre1 (F1) har nærmeste uttak fremover i tid.
        // Nærmeste dato for F1 er 11 dager frem i tid, men denne perioden har
        // utbetaling lik 0. velger bakover i tid.
        // Nærmeste dato for F1 blir dermed bakover i tid 10 + 14 = 24 dager.
        // Nærmeste dato for F2 er 7 dager tilbake i tid, men pga bufferet trekker fra
        // 14 slik at denne avstanden blir regnet som 21 dager
        // Nærmeste dato F2 blir dermed frem i tid: 14 dager.
        var uttakF1 = new ForeldrepengerUttak(lagPerioderDerAndrePeriodeHarUtbetalingLik0(LocalDate.now().minusDays(3)));
        var uttakF2 = new ForeldrepengerUttak(lagPerioderMedFullUtbetaling(LocalDate.now()));

        when(uttakTjeneste.hentHvisEksisterer(Mockito.eq(behandlingF1.getId()))).thenReturn(Optional.of(uttakF1));
        when(uttakTjeneste.hentHvisEksisterer(Mockito.eq(behandlingF2.getId()))).thenReturn(Optional.of(uttakF2));

        toForeldreBarnDødTjeneste = new ToForeldreBarnDødTjeneste(uttakTjeneste);

        // Act
        var behandlingSomSkalRevurderes = toForeldreBarnDødTjeneste.finnBehandlingSomSkalRevurderes(behandlingF1, behandlingF2);

        // Assert
        assertThat(behandlingSomSkalRevurderes).isEqualTo(behandlingF2);
    }

    @Test
    void skal_velge_behandling_f1_når_f2_ikke_har_uttaksresultat() {
        // Arrange
        var uttakF1 = new ForeldrepengerUttak(lagPerioderMedFullUtbetaling(LocalDate.now()));
        when(uttakTjeneste.hentHvisEksisterer(Mockito.eq(behandlingF1.getId()))).thenReturn(Optional.of(uttakF1));
        when(uttakTjeneste.hentHvisEksisterer(Mockito.eq(behandlingF2.getId()))).thenReturn(Optional.empty());
        toForeldreBarnDødTjeneste = new ToForeldreBarnDødTjeneste(uttakTjeneste);

        // Act
        var behandlingSomSkalRevurderes = toForeldreBarnDødTjeneste.finnBehandlingSomSkalRevurderes(behandlingF1, behandlingF2);

        // Assert
        assertThat(behandlingSomSkalRevurderes).isEqualTo(behandlingF1);
    }

    @Test
    void skal_velge_behandling_f2_når_f1_ikke_har_uttaksresultat() {
        // Arrange
        var uttakF2 = new ForeldrepengerUttak(lagPerioderMedFullUtbetaling(LocalDate.now()));
        when(uttakTjeneste.hentHvisEksisterer(Mockito.eq(behandlingF1.getId()))).thenReturn(Optional.empty());
        when(uttakTjeneste.hentHvisEksisterer(Mockito.eq(behandlingF2.getId()))).thenReturn(Optional.of(uttakF2));
        toForeldreBarnDødTjeneste = new ToForeldreBarnDødTjeneste(uttakTjeneste);

        // Act
        var behandlingSomSkalRevurderes = toForeldreBarnDødTjeneste.finnBehandlingSomSkalRevurderes(behandlingF1, behandlingF2);

        // Assert
        assertThat(behandlingSomSkalRevurderes).isEqualTo(behandlingF2);
    }

    @Test
    void skal_velge_behandling_f1_når_ingen_av_behandlingene_har_uttaksresultat() {
        // Arrange
        when(uttakTjeneste.hentHvisEksisterer(Mockito.eq(behandlingF1.getId()))).thenReturn(Optional.empty());
        when(uttakTjeneste.hentHvisEksisterer(Mockito.eq(behandlingF2.getId()))).thenReturn(Optional.empty());
        toForeldreBarnDødTjeneste = new ToForeldreBarnDødTjeneste(uttakTjeneste);

        // Act
        var behandlingSomSkalRevurderes = toForeldreBarnDødTjeneste.finnBehandlingSomSkalRevurderes(behandlingF1, behandlingF2);

        // Assert
        assertThat(behandlingSomSkalRevurderes).isEqualTo(behandlingF1);
    }

    private List<ForeldrepengerUttakPeriode> lagPerioderDerAndrePeriodeHarUtbetalingLik0(LocalDate midtDato) {
        return List.of(
                lagPeriodeMedUtbetalingsgrad(midtDato.minusWeeks(3), midtDato.minusWeeks(1), 100L),
                lagPeriodeMedUtbetalingsgrad(midtDato.plusWeeks(2), midtDato.plusWeeks(6), 0L));
    }

    private List<ForeldrepengerUttakPeriode> lagPerioderMedFullUtbetaling(LocalDate midtDato) {
        return List.of(
                lagPeriodeMedUtbetalingsgrad(midtDato.minusWeeks(3), midtDato.minusWeeks(1), 100L),
                lagPeriodeMedUtbetalingsgrad(midtDato.plusWeeks(2), midtDato.plusWeeks(6), 100L));
    }

    private ForeldrepengerUttakPeriode lagPeriodeMedUtbetalingsgrad(LocalDate fom, LocalDate tom, Long utbetalingsgrad) {
        var aktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
                .medArbeidsprosent(BigDecimal.valueOf(100))
                .medUtbetalingsgrad(new Utbetalingsgrad(utbetalingsgrad))
                .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
                .build();
        return new ForeldrepengerUttakPeriode.Builder()
                .medTidsperiode(fom, tom)
                .medResultatType(PeriodeResultatType.INNVILGET)
                .medAktiviteter(List.of(aktivitet))
                .build();
    }
}
