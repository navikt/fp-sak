package no.nav.foreldrepenger.datavarehus.v2;

import static no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType.FARA;
import static no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType.MORA;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkUttakPeriode.*;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkUttakPeriodeMapper.mapUttakPeriode;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.RettighetType.ALENEOMSORG;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.RettighetType.BARE_SØKER_RETT;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.RettighetType.BEGGE_RETT;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;

class StønadsstatistikkUttakPeriodeMapperTest {

    @Test
    void skal_velge_gradert_aktivitet() {
        var fom = LocalDate.of(2023, 12, 5);
        var annenAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS, null, null))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(new Utbetalingsgrad(100))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medSøktGraderingForAktivitetIPeriode(false)
            .medTrekkdager(new Trekkdager(1))
            .build();
        var gradertAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, null, null))
            .medArbeidsprosent(BigDecimal.TEN)
            .medUtbetalingsgrad(new Utbetalingsgrad(90))
            .medTrekkonto(StønadskontoType.FEDREKVOTE)
            .medSøktGraderingForAktivitetIPeriode(true)
            .medTrekkdager(new Trekkdager(BigDecimal.valueOf(0.9)))
            .build();
        var uttakPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fom, fom)
            .medGraderingInnvilget(true)
            .medSøktKonto(UttakPeriodeType.FEDREKVOTE)
            .medResultatÅrsak(PeriodeResultatÅrsak.GRADERING_KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(gradertAktivitet, annenAktivitet))
            .build();
        var stønadsstatistikkUttakPeriode = mapUttakPeriode(FARA,
            BEGGE_RETT, uttakPeriode);

        assertThat(stønadsstatistikkUttakPeriode.stønadskontoType()).isEqualTo(StønadsstatistikkVedtak.StønadskontoType.FEDREKVOTE);
        assertThat(stønadsstatistikkUttakPeriode.trekkdager().antall()).isEqualByComparingTo(BigDecimal.valueOf(0.9));
        assertThat(stønadsstatistikkUttakPeriode.gradering().aktivitetType()).isEqualTo(AktivitetType.NÆRING);
        assertThat(stønadsstatistikkUttakPeriode.gradering().arbeidsprosent()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void skal_sette_rettighet_ved_overføring() {
        var fom = LocalDate.of(2023, 12, 5);
        var uttakPeriodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS, null, null))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(Utbetalingsgrad.FULL)
            .medTrekkonto(StønadskontoType.FEDREKVOTE)
            .medTrekkdager(new Trekkdager(1))
            .build();
        var uttakPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fom, fom)
            .medSøktKonto(UttakPeriodeType.FEDREKVOTE)
            .medOverføringÅrsak(OverføringÅrsak.ALENEOMSORG)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.OVERFØRING_SØKER_HAR_ALENEOMSORG_FOR_BARNET)
            .medAktiviteter(List.of(uttakPeriodeAktivitet))
            .build();
        var stønadsstatistikkUttakPeriode = mapUttakPeriode(MORA,
            BEGGE_RETT, uttakPeriode);

        assertThat(stønadsstatistikkUttakPeriode.rettighetType()).isEqualTo(ALENEOMSORG);
        assertThat(stønadsstatistikkUttakPeriode.forklaring()).isEqualTo(Forklaring.OVERFØRING_ALENEOMSORG);
    }

    @Test
    void skal_sette_rettighet_ved_innvilget_annen_parts_kvote_uten_søkt_overføring() {
        var fom = LocalDate.of(2023, 12, 5);
        var uttakPeriodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS, null, null))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(Utbetalingsgrad.FULL)
            .medTrekkonto(StønadskontoType.FEDREKVOTE)
            .medTrekkdager(new Trekkdager(1))
            .build();
        var uttakPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fom, fom)
            .medSøktKonto(UttakPeriodeType.MØDREKVOTE)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.OVERFØRING_ANNEN_PART_HAR_IKKE_RETT_TIL_FORELDREPENGER)
            .medAktiviteter(List.of(uttakPeriodeAktivitet))
            .build();
        var stønadsstatistikkUttakPeriode = mapUttakPeriode(MORA,
            BEGGE_RETT, uttakPeriode);

        assertThat(stønadsstatistikkUttakPeriode.rettighetType()).isEqualTo(BARE_SØKER_RETT);
        assertThat(stønadsstatistikkUttakPeriode.forklaring()).isEqualTo(Forklaring.OVERFØRING_BARE_SØKER_RETT);
        assertThat(stønadsstatistikkUttakPeriode.type()).isEqualTo(PeriodeType.UTTAK);
    }

    @Test
    void skal_gi_forklaring_på_innvilget_utsettelse() {
        var fom = LocalDate.of(2023, 12, 5);
        var uttakPeriodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, Arbeidsgiver.virksomhet("123"), null))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medTrekkdager(Trekkdager.ZERO)
            .build();
        var uttakPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fom, fom)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medUtsettelseType(UttakUtsettelseType.BARN_INNLAGT)
            .medResultatÅrsak(PeriodeResultatÅrsak.UTSETTELSE_GYLDIG_PGA_BARN_INNLAGT)
            .medAktiviteter(List.of(uttakPeriodeAktivitet))
            .build();
        var stønadsstatistikkUttakPeriode = mapUttakPeriode(MORA, BEGGE_RETT, uttakPeriode);

        assertThat(stønadsstatistikkUttakPeriode.forklaring()).isEqualTo(Forklaring.UTSETTELSE_BARNINNLAGT);
        assertThat(stønadsstatistikkUttakPeriode.type()).isEqualTo(PeriodeType.UTSETTELSE);
        assertThat(stønadsstatistikkUttakPeriode.stønadskontoType()).isNull();
    }

    @Test
    void skal_gi_forklaring_på_mors_aktivitet_bfhr() {
        var fom = LocalDate.of(2023, 12, 5);
        var uttakPeriodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, Arbeidsgiver.virksomhet("123"), null))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(Utbetalingsgrad.FULL)
            .medTrekkdager(new Trekkdager(1))
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .build();
        var uttakPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fom, fom)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.FORELDREPENGER_KUN_FAR_HAR_RETT)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .medAktiviteter(List.of(uttakPeriodeAktivitet))
            .medSøktKonto(UttakPeriodeType.FORELDREPENGER)
            .build();
        var stønadsstatistikkUttakPeriode = mapUttakPeriode(FARA, BARE_SØKER_RETT, uttakPeriode);

        assertThat(stønadsstatistikkUttakPeriode.forklaring()).isEqualTo(Forklaring.AKTIVITETSKRAV_ARBEID);
    }

    @Test
    void skal_gi_forklaring_på_mors_aktivitet_begge_rett() {
        var fom = LocalDate.of(2023, 12, 5);
        var uttakPeriodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, Arbeidsgiver.virksomhet("123"), null))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(Utbetalingsgrad.FULL)
            .medTrekkdager(new Trekkdager(1))
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .build();
        var uttakPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fom, fom)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER)
            .medMorsAktivitet(MorsAktivitet.UTDANNING)
            .medAktiviteter(List.of(uttakPeriodeAktivitet))
            .medSøktKonto(UttakPeriodeType.FORELDREPENGER)
            .build();
        var stønadsstatistikkUttakPeriode = mapUttakPeriode(FARA, BEGGE_RETT, uttakPeriode);

        assertThat(stønadsstatistikkUttakPeriode.forklaring()).isEqualTo(Forklaring.AKTIVITETSKRAV_UTDANNING);
    }

    @Test
    void skal_gi_forklaring_på_minsterett() {
        var fom = LocalDate.of(2023, 12, 5);
        var uttakPeriodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, Arbeidsgiver.virksomhet("123"), null))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(Utbetalingsgrad.FULL)
            .medTrekkdager(new Trekkdager(1))
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .build();
        var uttakPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fom, fom)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.FORELDREPENGER_KUN_FAR_HAR_RETT_MOR_UFØR)
            .medMorsAktivitet(MorsAktivitet.IKKE_OPPGITT)
            .medAktiviteter(List.of(uttakPeriodeAktivitet))
            .medSøktKonto(UttakPeriodeType.FORELDREPENGER)
            .build();
        var stønadsstatistikkUttakPeriode = mapUttakPeriode(FARA, BARE_SØKER_RETT, uttakPeriode);

        assertThat(stønadsstatistikkUttakPeriode.forklaring()).isEqualTo(Forklaring.MINSTERETT);
    }

    @Test
    void skal_gi_forklaring_på_flerbarnsdager() {
        var fom = LocalDate.of(2023, 12, 5);
        var uttakPeriodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, Arbeidsgiver.virksomhet("123"), null))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(Utbetalingsgrad.FULL)
            .medTrekkdager(new Trekkdager(1))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .build();
        var uttakPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fom, fom)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER)
            .medMorsAktivitet(MorsAktivitet.IKKE_OPPGITT)
            .medAktiviteter(List.of(uttakPeriodeAktivitet))
            .medSøktKonto(UttakPeriodeType.FELLESPERIODE)
            .medFlerbarnsdager(true)
            .build();
        var stønadsstatistikkUttakPeriode = mapUttakPeriode(FARA, BEGGE_RETT, uttakPeriode);

        assertThat(stønadsstatistikkUttakPeriode.forklaring()).isEqualTo(Forklaring.FLERBARNSDAGER);
    }

    @Test
    void skal_gi_forklaring_på_samtidig_uttak() {
        var fom = LocalDate.of(2023, 12, 5);
        var uttakPeriodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, Arbeidsgiver.virksomhet("123"), null))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(new Utbetalingsgrad(50))
            .medTrekkdager(new Trekkdager(1))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .build();
        var uttakPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fom, fom)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER)
            .medAktiviteter(List.of(uttakPeriodeAktivitet))
            .medSøktKonto(UttakPeriodeType.FELLESPERIODE)
            .medMorsAktivitet(MorsAktivitet.IKKE_OPPGITT)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(new SamtidigUttaksprosent(50))
            .build();
        var stønadsstatistikkUttakPeriode = mapUttakPeriode(FARA, BEGGE_RETT, uttakPeriode);

        assertThat(stønadsstatistikkUttakPeriode.forklaring()).isEqualTo(Forklaring.SAMTIDIG_MØDREKVOTE);
    }

    @Test
    void skal_mappe_manglende_søkte_perioder() {
        var fom = LocalDate.of(2019, 12, 5);
        var uttakPeriodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, Arbeidsgiver.virksomhet("123"), null))
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medTrekkdager(new Trekkdager(5))
            .medTrekkonto(StønadskontoType.FEDREKVOTE)
            .build();
        var uttakPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fom, fom.plusWeeks(1).minusDays(1))
            .medResultatType(PeriodeResultatType.AVSLÅTT)
            .medResultatÅrsak(PeriodeResultatÅrsak.HULL_MELLOM_FORELDRENES_PERIODER)
            .medAktiviteter(List.of(uttakPeriodeAktivitet))
            .medSøktKonto(UttakPeriodeType.UDEFINERT)
            .build();
        var stønadsstatistikkUttakPeriode = mapUttakPeriode(FARA, BEGGE_RETT, uttakPeriode);

        assertThat(stønadsstatistikkUttakPeriode.stønadskontoType()).isEqualTo(StønadsstatistikkVedtak.StønadskontoType.FEDREKVOTE);
        assertThat(stønadsstatistikkUttakPeriode.erUtbetaling()).isFalse();
        assertThat(stønadsstatistikkUttakPeriode.trekkdager().antall()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(stønadsstatistikkUttakPeriode.type()).isEqualTo(PeriodeType.AVSLAG);
        assertThat(stønadsstatistikkUttakPeriode.forklaring()).isEqualTo(Forklaring.AVSLAG_IKKE_SØKT);
    }
}
