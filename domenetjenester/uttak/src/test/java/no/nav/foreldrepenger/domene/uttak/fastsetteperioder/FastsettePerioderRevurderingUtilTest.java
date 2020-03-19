package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.uttak.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.TrekkdagerUtregningUtil;
import no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Periode;

public class FastsettePerioderRevurderingUtilTest {


    @Test
    public void skalSplitteOppUttaksresultatPeriodeHvisEndringsdatoErIPerioden() {
        UttakResultatPerioderEntitet opprinneligePerioder = new UttakResultatPerioderEntitet();
        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();

        UttakResultatPeriodeEntitet periode1 = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2018, 6, 6),
            LocalDate.of(2018, 6, 20))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        leggTilPeriodeAktivitet(uttakAktivitet, periode1, StønadskontoType.MØDREKVOTE, new Trekkdager(10));
        UttakResultatPeriodeEntitet periode2 = new UttakResultatPeriodeEntitet.Builder(periode1.getTom().plusDays(1), periode1.getTom().plusWeeks(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        leggTilPeriodeAktivitet(uttakAktivitet, periode2, StønadskontoType.FELLESPERIODE, new Trekkdager(10));

        opprinneligePerioder.leggTilPeriode(periode1);
        opprinneligePerioder.leggTilPeriode(periode2);
        UttakResultatEntitet opprinneligUttak = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(opprinneligePerioder)
            .build();

        LocalDate endringsdato = periode2.getFom().plusDays(2);
        List<UttakResultatPeriodeEntitet> perioder = FastsettePerioderRevurderingUtil.perioderFørDato(opprinneligUttak, endringsdato);

        assertThat(perioder).hasSize(2);
        assertThat(perioder.get(0).getFom()).isEqualTo(periode1.getFom());
        assertThat(perioder.get(0).getTom()).isEqualTo(periode1.getTom());
        assertThat(perioder.get(0).getAktiviteter().get(0).getTrekkdager()).isEqualTo(periode1.getAktiviteter().get(0).getTrekkdager());
        assertThat(perioder.get(1).getFom()).isEqualTo(periode2.getFom());
        assertThat(perioder.get(1).getTom()).isEqualTo(endringsdato.minusDays(1));
        Trekkdager forventetTrekkdagerSplittetPeriode = new Trekkdager(TrekkdagerUtregningUtil.trekkdagerFor(
            new Periode(perioder.get(1).getFom(), perioder.get(1).getTom()), false, BigDecimal.ZERO, null
        ).decimalValue());
        assertThat(perioder.get(1).getAktiviteter().get(0).getTrekkdager()).isEqualTo(forventetTrekkdagerSplittetPeriode);
    }

    @Test
    public void skalIkkeKopiereUttaksperioderTattAvAnnenpartFraForrigeUttaksresultat() {
        UttakResultatPerioderEntitet opprinneligePerioder = new UttakResultatPerioderEntitet();
        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();

        UttakResultatPeriodeEntitet periode1 = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2018, 6, 6),
            LocalDate.of(2018, 6, 20))
            .medPeriodeResultat(PeriodeResultatType.AVSLÅTT, IkkeOppfyltÅrsak.DEN_ANDRE_PART_HAR_OVERLAPPENDE_UTTAKSPERIODER_SOM_ER_INNVILGET_UTSETTELSE)
            .build();
        leggTilPeriodeAktivitet(uttakAktivitet, periode1, StønadskontoType.MØDREKVOTE, Trekkdager.ZERO, BigDecimal.ZERO);
        UttakResultatPeriodeEntitet periode2 = new UttakResultatPeriodeEntitet.Builder(periode1.getTom().plusDays(1), periode1.getTom().plusDays(10))
            .medPeriodeResultat(PeriodeResultatType.AVSLÅTT, IkkeOppfyltÅrsak.DEN_ANDRE_PART_OVERLAPPENDE_UTTAK_IKKE_SØKT_INNVILGET_SAMTIDIG_UTTAK)
            .build();
        leggTilPeriodeAktivitet(uttakAktivitet, periode2, StønadskontoType.MØDREKVOTE, Trekkdager.ZERO, BigDecimal.ZERO);

        opprinneligePerioder.leggTilPeriode(periode1);
        opprinneligePerioder.leggTilPeriode(periode2);
        UttakResultatEntitet opprinneligUttak = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(opprinneligePerioder)
            .build();

        LocalDate endringsdato = periode2.getTom();
        List<UttakResultatPeriodeEntitet> perioder = FastsettePerioderRevurderingUtil.perioderFørDato(opprinneligUttak, endringsdato);

        assertThat(perioder).isEmpty();
    }

    @Test
    public void skalRegneUtTrekkdagerKorrektVedSplittingHvisIkkeSamtidigUttakMenHarSamtidigUttaksprosent() {
        UttakResultatPerioderEntitet opprinneligePerioder = new UttakResultatPerioderEntitet();
        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();

        //Fått perioder med samtidig uttak false, men med en prosent i prod
        UttakResultatPeriodeEntitet opprinneligPeriode = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2019, 4, 22),
            LocalDate.of(2019, 4, 26))
            .medSamtidigUttaksprosent(BigDecimal.ZERO)
            .medSamtidigUttak(false)
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        leggTilPeriodeAktivitet(uttakAktivitet, opprinneligPeriode, StønadskontoType.MØDREKVOTE, new Trekkdager(10));

        opprinneligePerioder.leggTilPeriode(opprinneligPeriode);
        UttakResultatEntitet opprinneligUttak = new UttakResultatEntitet.Builder(mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(opprinneligePerioder)
            .build();

        LocalDate endringsdato = opprinneligPeriode.getFom().plusDays(2);
        List<UttakResultatPeriodeEntitet> perioder = FastsettePerioderRevurderingUtil.perioderFørDato(opprinneligUttak, endringsdato);

        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getAktiviteter().get(0).getTrekkdager()).isEqualTo(new Trekkdager(2));
    }

    private void leggTilPeriodeAktivitet(UttakAktivitetEntitet uttakAktivitet, UttakResultatPeriodeEntitet periode, StønadskontoType stønadskontoType, Trekkdager trekkdager) {
        leggTilPeriodeAktivitet(uttakAktivitet, periode, stønadskontoType, trekkdager, BigDecimal.TEN);
    }

    private void leggTilPeriodeAktivitet(UttakAktivitetEntitet uttakAktivitet, UttakResultatPeriodeEntitet periode, StønadskontoType stønadskontoType, Trekkdager trekkdager, BigDecimal utbetalingsgrad) {
        UttakResultatPeriodeAktivitetEntitet periodeAktivitet1 = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet)
            .medTrekkonto(stønadskontoType)
            .medTrekkdager(trekkdager)
            .medArbeidsprosent(utbetalingsgrad)
            .build();
        periode.leggTilAktivitet(periodeAktivitet1);
    }

}
