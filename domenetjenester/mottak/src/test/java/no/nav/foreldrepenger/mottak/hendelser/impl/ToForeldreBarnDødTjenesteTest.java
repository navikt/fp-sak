package no.nav.foreldrepenger.mottak.hendelser.impl;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.mottak.hendelser.ToForeldreBarnDødTjeneste;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;


@RunWith(CdiRunner.class)
public class ToForeldreBarnDødTjenesteTest {

    @Mock
    private UttakRepository uttakRepository;

    private ToForeldreBarnDødTjeneste toForeldreBarnDødTjeneste;

    private Behandling behandlingF1;
    private Behandling behandlingF2;
    private UttakResultatEntitet uttakResultatEntitetF1;
    private UttakResultatEntitet uttakResultatEntitetF2;



    @Before
    public void oppsett() {
        ScenarioMorSøkerForeldrepenger scenarioF1 = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenarioF1.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        ScenarioFarSøkerForeldrepenger scenarioF2 = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenarioF2.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        behandlingF1 = scenarioF1.lagMocked();
        behandlingF2 = scenarioF2.lagMocked();
        uttakRepository = mock(UttakRepository.class);


    }

    @Test
    public void skal_velge_behandling_f1_når_f1_har_aktivt_uttak() {
        // Arrange Foreldre1 (F1) har aktivt uttak nå, og velges automatisk
        UttakResultatEntitet.Builder uttakResultatEntitetF1Builder = new UttakResultatEntitet.Builder(behandlingF1.getBehandlingsresultat());
        UttakResultatEntitet.Builder uttakResultatEntitetF2Builder = new UttakResultatEntitet.Builder(behandlingF2.getBehandlingsresultat());
        uttakResultatEntitetF1 =  uttakResultatEntitetF1Builder.medOpprinneligPerioder(lagPerioderMedFullUtbetaling(LocalDate.now().minusDays(15))).build();
        uttakResultatEntitetF2 = uttakResultatEntitetF2Builder.medOpprinneligPerioder(lagPerioderMedFullUtbetaling(LocalDate.now())).build();

        when(uttakRepository.hentUttakResultat(Mockito.eq(behandlingF1.getId()))).thenReturn(uttakResultatEntitetF1);
        when(uttakRepository.hentUttakResultat(Mockito.eq(behandlingF2.getId()))).thenReturn(uttakResultatEntitetF2);

        toForeldreBarnDødTjeneste = new ToForeldreBarnDødTjeneste(uttakRepository);


        // Act
        Behandling behandlingSomSkalRevurderes = toForeldreBarnDødTjeneste.finnBehandlingSomSkalRevurderes(behandlingF1,behandlingF2);

        // Assert
        assertThat(behandlingSomSkalRevurderes).isEqualTo(behandlingF1);
    }


    @Test
    public void skal_velge_behandling_f1_når_f1_har_nærmeste_uttak() {
        // Arrange Foreldre1 (F1) har nærmeste uttak fremover i tid.
        // Nærmeste dato for F1 er 11 dager frem i tid;
        // Nærmeste dato for F2 er 7 dager tilbake i tid, men pga bufferet trekker fra 14 slik at denne avstanden blir regnet som 21 dager
        // Nærmeste dato F2 blir dermed frem i tid: 14 dager.
        UttakResultatEntitet.Builder uttakResultatEntitetF1Builder = new UttakResultatEntitet.Builder(behandlingF1.getBehandlingsresultat());
        UttakResultatEntitet.Builder uttakResultatEntitetF2Builder = new UttakResultatEntitet.Builder(behandlingF2.getBehandlingsresultat());
        uttakResultatEntitetF1 =  uttakResultatEntitetF1Builder.medOpprinneligPerioder(lagPerioderMedFullUtbetaling(LocalDate.now().minusDays(3))).build();
        uttakResultatEntitetF2 = uttakResultatEntitetF2Builder.medOpprinneligPerioder(lagPerioderMedFullUtbetaling(LocalDate.now())).build();

        when(uttakRepository.hentUttakResultat(Mockito.eq(behandlingF1.getId()))).thenReturn(uttakResultatEntitetF1);
        when(uttakRepository.hentUttakResultat(Mockito.eq(behandlingF2.getId()))).thenReturn(uttakResultatEntitetF2);

        toForeldreBarnDødTjeneste = new ToForeldreBarnDødTjeneste(uttakRepository);


        // Act
        Behandling behandlingSomSkalRevurderes = toForeldreBarnDødTjeneste.finnBehandlingSomSkalRevurderes(behandlingF1,behandlingF2);

        // Assert
        assertThat(behandlingSomSkalRevurderes).isEqualTo(behandlingF1);
    }


    @Test
    public void skal_velge_behandling_f2_når_f1_har_nærmeste_uttak_men_ingen_utbetaling() {
        // Arrange Foreldre1 (F1) har nærmeste uttak fremover i tid.
        // Nærmeste dato for F1 er 11 dager frem i tid, men denne perioden har utbetaling lik 0. velger bakover i tid.
        // Nærmeste dato for F1 blir dermed bakover i tid 10 + 14 = 24 dager.
        // Nærmeste dato for F2 er 7 dager tilbake i tid, men pga bufferet trekker fra 14 slik at denne avstanden blir regnet som 21 dager
        // Nærmeste dato F2 blir dermed frem i tid: 14 dager.

        UttakResultatEntitet.Builder uttakResultatEntitetF1Builder = new UttakResultatEntitet.Builder(behandlingF1.getBehandlingsresultat());
        UttakResultatEntitet.Builder uttakResultatEntitetF2Builder = new UttakResultatEntitet.Builder(behandlingF2.getBehandlingsresultat());
        uttakResultatEntitetF1 =  uttakResultatEntitetF1Builder.medOpprinneligPerioder(lagPerioderDerAndrePeriodeHarUtbetalingLik0(LocalDate.now().minusDays(3))).build();
        uttakResultatEntitetF2 = uttakResultatEntitetF2Builder.medOpprinneligPerioder(lagPerioderMedFullUtbetaling(LocalDate.now())).build();

        when(uttakRepository.hentUttakResultat(Mockito.eq(behandlingF1.getId()))).thenReturn(uttakResultatEntitetF1);
        when(uttakRepository.hentUttakResultat(Mockito.eq(behandlingF2.getId()))).thenReturn(uttakResultatEntitetF2);

        toForeldreBarnDødTjeneste = new ToForeldreBarnDødTjeneste(uttakRepository);


        // Act
        Behandling behandlingSomSkalRevurderes = toForeldreBarnDødTjeneste.finnBehandlingSomSkalRevurderes(behandlingF1,behandlingF2);

        // Assert
        assertThat(behandlingSomSkalRevurderes).isEqualTo(behandlingF2);
    }




    private UttakResultatPerioderEntitet lagPerioderDerAndrePeriodeHarUtbetalingLik0(LocalDate midtDato){
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(lagPeriodeMedUtbetalingsProsent(midtDato.minusWeeks(3), midtDato.minusWeeks(1), 100L));
        perioder.leggTilPeriode(lagPeriodeMedUtbetalingsProsent(midtDato.plusWeeks(2), midtDato.plusWeeks(6), 0L));
        return perioder;
    }


    private UttakResultatPerioderEntitet lagPerioderMedFullUtbetaling(LocalDate midtDato){
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(lagPeriodeMedUtbetalingsProsent(midtDato.minusWeeks(3), midtDato.minusWeeks(1), 100L));
        perioder.leggTilPeriode(lagPeriodeMedUtbetalingsProsent(midtDato.plusWeeks(2), midtDato.plusWeeks(6), 100L));
        return perioder;
    }


    private UttakResultatPeriodeEntitet lagPeriodeMedUtbetalingsProsent(LocalDate fom, LocalDate tom, Long utbetalingsprosent) {
        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(fom, tom).medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
        UttakAktivitetEntitet uttakAktivitetEntitet = mock(UttakAktivitetEntitet.class);
        UttakResultatPeriodeAktivitetEntitet aktivitetEntitet = UttakResultatPeriodeAktivitetEntitet.builder(periode, uttakAktivitetEntitet)
            .medArbeidsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsprosent(BigDecimal.valueOf(utbetalingsprosent)).build();
        periode.leggTilAktivitet(aktivitetEntitet);
        return periode;
    }
}
