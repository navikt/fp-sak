package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.TrekkdagerUtregningUtil;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Periode;

public class FastsettePerioderRevurderingUtilTest {


    @Test
    public void skalSplitteOppUttaksresultatPeriodeHvisEndringsdatoErIPerioden() {
        var opprinneligePerioder = new UttakResultatPerioderEntitet();
        var uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();

        var periode1 = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2018, 6, 6),
            LocalDate.of(2018, 6, 20))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        leggTilPeriodeAktivitet(uttakAktivitet, periode1, StønadskontoType.MØDREKVOTE, new Trekkdager(10));
        var periode2 = new UttakResultatPeriodeEntitet.Builder(periode1.getTom().plusDays(1), periode1.getTom().plusWeeks(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        leggTilPeriodeAktivitet(uttakAktivitet, periode2, StønadskontoType.FELLESPERIODE, new Trekkdager(10));

        opprinneligePerioder.leggTilPeriode(periode1);
        opprinneligePerioder.leggTilPeriode(periode2);
        var opprinneligUttak = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(opprinneligePerioder)
            .build();

        var endringsdato = periode2.getFom().plusDays(2);
        var perioder = FastsettePerioderRevurderingUtil.perioderFørDato(opprinneligUttak, endringsdato);

        assertThat(perioder).hasSize(2);
        assertThat(perioder.get(0).getFom()).isEqualTo(periode1.getFom());
        assertThat(perioder.get(0).getTom()).isEqualTo(periode1.getTom());
        assertThat(perioder.get(0).getAktiviteter().get(0).getTrekkdager()).isEqualTo(periode1.getAktiviteter().get(0).getTrekkdager());
        assertThat(perioder.get(1).getFom()).isEqualTo(periode2.getFom());
        assertThat(perioder.get(1).getTom()).isEqualTo(endringsdato.minusDays(1));
        var forventetTrekkdagerSplittetPeriode = new Trekkdager(TrekkdagerUtregningUtil.trekkdagerFor(
            new Periode(perioder.get(1).getFom(), perioder.get(1).getTom()), false, BigDecimal.ZERO, null
        ).decimalValue());
        assertThat(perioder.get(1).getAktiviteter().get(0).getTrekkdager()).isEqualTo(forventetTrekkdagerSplittetPeriode);
    }

    @Test
    public void skalIkkeKopiereUttaksperioderTattAvAnnenpartFraForrigeUttaksresultat() {
        var opprinneligePerioder = new UttakResultatPerioderEntitet();
        var uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();

        var periode1 = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2018, 6, 6),
            LocalDate.of(2018, 6, 20))
            .medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.DEN_ANDRE_PART_HAR_OVERLAPPENDE_UTTAKSPERIODER_SOM_ER_INNVILGET_UTSETTELSE)
            .build();
        leggTilPeriodeAktivitet(uttakAktivitet, periode1, StønadskontoType.MØDREKVOTE, Trekkdager.ZERO, BigDecimal.ZERO);
        var periode2 = new UttakResultatPeriodeEntitet.Builder(periode1.getTom().plusDays(1), periode1.getTom().plusDays(10))
            .medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIG_UTTAK)
            .build();
        leggTilPeriodeAktivitet(uttakAktivitet, periode2, StønadskontoType.MØDREKVOTE, Trekkdager.ZERO, BigDecimal.ZERO);

        opprinneligePerioder.leggTilPeriode(periode1);
        opprinneligePerioder.leggTilPeriode(periode2);
        var opprinneligUttak = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(opprinneligePerioder)
            .build();

        var endringsdato = periode2.getTom();
        var perioder = FastsettePerioderRevurderingUtil.perioderFørDato(opprinneligUttak, endringsdato);

        assertThat(perioder).isEmpty();
    }

    @Test
    public void skalRegneUtTrekkdagerKorrektVedSplittingHvisIkkeSamtidigUttakMenHarSamtidigUttaksprosent() {
        var opprinneligePerioder = new UttakResultatPerioderEntitet();
        var uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();

        //Fått perioder med samtidig uttak false, men med en prosent i prod
        var opprinneligPeriode = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2019, 4, 22),
            LocalDate.of(2019, 4, 26))
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.ZERO)
            .medSamtidigUttak(false)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        leggTilPeriodeAktivitet(uttakAktivitet, opprinneligPeriode, StønadskontoType.MØDREKVOTE, new Trekkdager(10));

        opprinneligePerioder.leggTilPeriode(opprinneligPeriode);
        var opprinneligUttak = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(opprinneligePerioder)
            .build();

        var endringsdato = opprinneligPeriode.getFom().plusDays(2);
        var perioder = FastsettePerioderRevurderingUtil.perioderFørDato(opprinneligUttak, endringsdato);

        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager(2));
    }

    @Test
    public void skalFjerneUttaksperioderFørEndringsdatoSomBareErTommeHelger() {
        var opprinneligePerioder = new UttakResultatPerioderEntitet();
        var uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build();

        //Helg med trekkdager skal ikke oppstå i praksis
        var helgMedTrekkdager = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 10, 2))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        leggTilPeriodeAktivitet(uttakAktivitet, helgMedTrekkdager, StønadskontoType.MØDREKVOTE, new Trekkdager(10));
        var helgUtenTrekkdager = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2022, 10, 8), LocalDate.of(2022, 10, 9))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        leggTilPeriodeAktivitet(uttakAktivitet, helgUtenTrekkdager, StønadskontoType.FELLESPERIODE, Trekkdager.ZERO);

        opprinneligePerioder.leggTilPeriode(helgMedTrekkdager);
        opprinneligePerioder.leggTilPeriode(helgUtenTrekkdager);
        var opprinneligUttak = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(opprinneligePerioder)
            .build();

        var endringsdato = helgUtenTrekkdager.getTom().plusDays(1);
        var perioder = FastsettePerioderRevurderingUtil.perioderFørDato(opprinneligUttak, endringsdato);

        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getFom()).isEqualTo(helgMedTrekkdager.getFom());
        assertThat(perioder.get(0).getTom()).isEqualTo(helgMedTrekkdager.getTom());
    }

    private void leggTilPeriodeAktivitet(UttakAktivitetEntitet uttakAktivitet, UttakResultatPeriodeEntitet periode, StønadskontoType stønadskontoType, Trekkdager trekkdager) {
        leggTilPeriodeAktivitet(uttakAktivitet, periode, stønadskontoType, trekkdager, BigDecimal.TEN);
    }

    private void leggTilPeriodeAktivitet(UttakAktivitetEntitet uttakAktivitet, UttakResultatPeriodeEntitet periode, StønadskontoType stønadskontoType, Trekkdager trekkdager, BigDecimal utbetalingsgrad) {
        var periodeAktivitet1 = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet)
            .medTrekkonto(stønadskontoType)
            .medTrekkdager(trekkdager)
            .medArbeidsprosent(utbetalingsgrad)
            .build();
        periode.leggTilAktivitet(periodeAktivitet1);
    }

}
