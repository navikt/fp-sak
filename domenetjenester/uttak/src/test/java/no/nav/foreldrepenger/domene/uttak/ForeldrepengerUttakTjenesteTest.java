package no.nav.foreldrepenger.domene.uttak;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class ForeldrepengerUttakTjenesteTest {

    @Test
    void skal_mappe_uttak() {
        var repositoryProvider = new UttakRepositoryStubProvider();
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusMonths(6);
        var mottattDato = LocalDate.now().minusWeeks(2);
        var periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMottattDato(mottattDato)
            .medTidligstMottattDato(mottattDato.minusDays(1))
            .build();
        var periodeResultatType = PeriodeResultatType.INNVILGET;
        var innvilgetÅrsak = PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER;
        var lagretUttaksperiode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(periodeResultatType, innvilgetÅrsak).medPeriodeSoknad(periodeSøknad)
            .build();
        var uttakArbeidType = UttakArbeidType.FRILANS;
        var uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(uttakArbeidType)
            .build();
        var arbeidsprosent = BigDecimal.ZERO;
        var periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(lagretUttaksperiode, uttakAktivitet)
            .medArbeidsprosent(arbeidsprosent).build();
        lagretUttaksperiode.leggTilAktivitet(periodeAktivitet);
        var lagretUttak = new UttakResultatPerioderEntitet().leggTilPeriode(lagretUttaksperiode);
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medUttak(lagretUttak)
            .lagre(repositoryProvider);

        var tjeneste = new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository());

        var uttak = tjeneste.hentUttak(behandling.getId());

        assertThat(uttak.getGjeldendePerioder()).hasSize(1);
        var periode = uttak.getGjeldendePerioder().get(0);
        var aktiviteter = periode.getAktiviteter();
        assertThat(aktiviteter).hasSize(1);
        assertThat(periode.getMottattDato()).isEqualTo(mottattDato);
        assertThat(periode.getTidligstMottatttDato()).isEqualTo(mottattDato.minusDays(1));
        assertThat(periode.getResultatType()).isEqualTo(periodeResultatType);
        assertThat(periode.getResultatÅrsak()).isEqualTo(innvilgetÅrsak);
        assertThat(aktiviteter.get(0).getUttakArbeidType()).isEqualTo(uttakArbeidType);
        assertThat(aktiviteter.get(0).getArbeidsprosent()).isEqualTo(arbeidsprosent);
    }

}
