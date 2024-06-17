package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class VedtaksperiodeFilterFRITest {


    @Test
    void skalBeholdeSøknadsperioderDersomEtterUttak() {
        var fom = LocalDate.of(2022, 10, 10);
        var tom = LocalDate.of(2022, 11, 9);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef());
        var uttakPeriode1 = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET,
                PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER).build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1, arbeidsforhold1.build())
            .medTrekkdager(new Trekkdager(42))
            .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        perioder.leggTilPeriode(uttakPeriode1);
        var uttakResultat = new UttakResultatEntitet.Builder(Behandlingsresultat.builder().build()).medOpprinneligPerioder(perioder).build();

        // Ny periode
        var søknad = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(tom.plusDays(1), tom.plusWeeks(4))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();

        var filtrert = VedtaksperiodeFilter.filtrerVekkPerioderSomErLikeInnvilgetUttak(123L, List.of(søknad), uttakResultat, false);
        assertThat(filtrert).hasSize(1);
        assertThat(filtrert.get(0)).isEqualTo(søknad);
    }

    @Test
    void skalFiltrereVekkTidligSøknadsperiodeDersomHeltLikUttak() {
        var fom = LocalDate.of(2022, 10, 10);
        var tom = LocalDate.of(2022, 11, 9);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef());
        var uttakPeriode1 = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET,
                PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER).build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1, arbeidsforhold1.build())
            .medTrekkdager(new Trekkdager(42))
            .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        perioder.leggTilPeriode(uttakPeriode1);
        var uttakResultat = new UttakResultatEntitet.Builder(Behandlingsresultat.builder().build()).medOpprinneligPerioder(perioder).build();

        // Lik eksisterende vedtak
        var søknad1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        // Ny periode
        var søknad2 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(tom.plusDays(1), tom.plusWeeks(4))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();

        var filtrert = VedtaksperiodeFilter.filtrerVekkPerioderSomErLikeInnvilgetUttak(123L, List.of(søknad1, søknad2), uttakResultat, false);
        assertThat(filtrert).hasSize(1);
        assertThat(filtrert.get(0)).isEqualTo(søknad2);
    }

    @Test
    void skalLeggeInnFRISøknadsperiodeDersomHeltLikUttak() {
        var fom = LocalDate.of(2022, 10, 10);
        var tom = LocalDate.of(2022, 11, 9);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef());
        var uttakPeriode1 = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET,
                PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER).build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1, arbeidsforhold1.build())
            .medTrekkdager(new Trekkdager(42))
            .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        perioder.leggTilPeriode(uttakPeriode1);
        var uttakResultat = new UttakResultatEntitet.Builder(Behandlingsresultat.builder().build()).medOpprinneligPerioder(perioder).build();

        var søknad = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();

        var filtrert = VedtaksperiodeFilter.filtrerVekkPerioderSomErLikeInnvilgetUttak(123L, List.of(søknad), uttakResultat, false);
        assertThat(filtrert).hasSize(1);
        assertThat(filtrert.get(0).getFom()).isEqualTo(tom.plusDays(1));
        assertThat(filtrert.get(0).getTom()).isEqualTo(tom.plusDays(1));
        assertThat(filtrert.get(0).getÅrsak()).isEqualTo(UtsettelseÅrsak.FRI);
    }

    @Test
    void skalLeggeInnFRISøknadsperiodeDersomHullVedEndring() {
        var fom = LocalDate.of(2022, 10, 10);
        var tom = LocalDate.of(2022, 11, 9);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef());
        var uttakPeriode1 = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET,
                PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER).build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1, arbeidsforhold1.build())
            .medTrekkdager(new Trekkdager(42))
            .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        perioder.leggTilPeriode(uttakPeriode1);
        var uttakResultat = new UttakResultatEntitet.Builder(Behandlingsresultat.builder().build()).medOpprinneligPerioder(perioder).build();

        var søknad1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom.minusWeeks(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var søknad2 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(tom.plusWeeks(1), tom.plusWeeks(2))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();

        var filtrert = VedtaksperiodeFilter.filtrerVekkPerioderSomErLikeInnvilgetUttak(123L, List.of(søknad1, søknad2), uttakResultat, false);
        var forventetFri = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.FRI)
            .medPeriode(tom.minusWeeks(1).plusDays(1), tom.plusWeeks(1).minusDays(1))
            .build();

        assertThat(filtrert).hasSize(2);
        assertThat(filtrert.stream().anyMatch(p -> p.equals(forventetFri))).isTrue();
        assertThat(filtrert.stream().anyMatch(p -> p.equals(søknad2))).isTrue();
    }

    @Test
    void skalLeggeInnFRISøknadsperiodeDersomVedtakErLengerEnnSøknad() {
        var fom = LocalDate.of(2022, 10, 10);
        var tom = LocalDate.of(2022, 11, 9);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef());
        var uttakPeriode1 = new UttakResultatPeriodeEntitet.Builder(fom, tom.plusWeeks(4)).medResultatType(PeriodeResultatType.INNVILGET,
                PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER).build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1, arbeidsforhold1.build())
            .medTrekkdager(new Trekkdager(42))
            .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        perioder.leggTilPeriode(uttakPeriode1);
        var uttakResultat = new UttakResultatEntitet.Builder(Behandlingsresultat.builder().build()).medOpprinneligPerioder(perioder).build();

        var søknad = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();

        var filtrert = VedtaksperiodeFilter.filtrerVekkPerioderSomErLikeInnvilgetUttak(123L, List.of(søknad), uttakResultat, false);
        assertThat(filtrert).hasSize(1);
        assertThat(filtrert.get(0).getFom()).isEqualTo(tom.plusDays(1));
        assertThat(filtrert.get(0).getTom()).isEqualTo(tom.plusWeeks(4));
        assertThat(filtrert.get(0).getÅrsak()).isEqualTo(UtsettelseÅrsak.FRI);
    }

    @Test
    void skalAvkorteSøknadsperiodeDersomStrekkerSegForbiInnvilgetUttak() {
        var fom = LocalDate.of(2022, 10, 10);
        var tom = LocalDate.of(2022, 11, 9);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef());
        var uttakPeriode1 = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET,
                PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medGraderingArbeidsprosent(BigDecimal.TEN)
                .medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1, arbeidsforhold1.build())
            .medTrekkdager(new Trekkdager(42))
            .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
            .medErSøktGradering(true)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        perioder.leggTilPeriode(uttakPeriode1);
        var uttakResultat = new UttakResultatEntitet.Builder(Behandlingsresultat.builder().build()).medOpprinneligPerioder(perioder).build();

        // Utvider søknadsperioden med 4 uker ift vedtak
        var søknad = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom.plusWeeks(4))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();

        var filtrert = VedtaksperiodeFilter.filtrerVekkPerioderSomErLikeInnvilgetUttak(123L, List.of(søknad), uttakResultat, false);
        assertThat(filtrert).hasSize(1);
        assertThat(filtrert.get(0).getFom()).isEqualTo(tom.plusDays(1));
    }

    @Test
    void leggerInnUtsettelseOgSenereUttak() {
        var fom = LocalDate.of(2022, 10, 10);
        var tom = LocalDate.of(2022, 11, 9);
        var utsattFom = LocalDate.of(2022, 11, 1);
        var senereUttakFom = LocalDate.of(2022, 12, 1);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef());
        var uttakPeriode1 = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET,
                PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER).build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1, arbeidsforhold1.build())
            .medTrekkdager(new Trekkdager(42))
            .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        perioder.leggTilPeriode(uttakPeriode1);
        var uttakResultat = new UttakResultatEntitet.Builder(Behandlingsresultat.builder().build()).medOpprinneligPerioder(perioder).build();

        // Lik eksisterende vedtak
        var søknad0 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, utsattFom.minusDays(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var søknad1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medÅrsak(UtsettelseÅrsak.FRI)
            .medPeriode(utsattFom, senereUttakFom.minusDays(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        // Ny periode
        var søknad2 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(senereUttakFom, senereUttakFom.plusDays(23))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();

        var filtrert = VedtaksperiodeFilter.filtrerVekkPerioderSomErLikeInnvilgetUttak(123L, List.of(søknad0, søknad1, søknad2), uttakResultat,
            false);
        assertThat(filtrert).hasSize(2);
        assertThat(filtrert.stream().anyMatch(p -> p.equals(søknad1))).isTrue();
        assertThat(filtrert.stream().anyMatch(p -> p.equals(søknad2))).isTrue();
    }

    @Test
    void utviderPeriodeUtenUttakOgLeggerTilSenereUttak() {
        var fom0 = LocalDate.of(2022, 10, 4);
        var fom = LocalDate.of(2022, 10, 10);
        var tom = LocalDate.of(2022, 11, 9);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .build();
        var uttakPeriode0 = new UttakResultatPeriodeEntitet.Builder(fom0, fom0.plusDays(1)).medResultatType(PeriodeResultatType.INNVILGET,
                PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER).build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode0, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(42))
            .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        perioder.leggTilPeriode(uttakPeriode0);
        var uttakPeriode1 = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET,
                PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER).build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(42))
            .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        perioder.leggTilPeriode(uttakPeriode1);
        var uttakResultat = new UttakResultatEntitet.Builder(Behandlingsresultat.builder().build()).medOpprinneligPerioder(perioder).build();

        // Utvider oppholdet mellom uttaket med et par dager
        var søknad0 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom0, fom0.plusDays(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        var søknad1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom.plusDays(3), tom.plusWeeks(2))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();

        var filtrert = VedtaksperiodeFilter.filtrerVekkPerioderSomErLikeInnvilgetUttak(123L, List.of(søknad0, søknad1), uttakResultat, false);
        assertThat(filtrert).hasSize(2);
        var forventetFri = OppgittPeriodeBuilder.ny().medÅrsak(UtsettelseÅrsak.FRI).medPeriode(fom, søknad1.getFom().minusDays(1)).build();
        assertThat(filtrert.stream().anyMatch(p -> p.equals(forventetFri))).isTrue();
        assertThat(filtrert.stream().anyMatch(p -> p.equals(søknad1))).isTrue();
    }

    @Test
    void prod_overstyrt_uttak_utsettelse_med_konto() {
        var fom = LocalDate.of(2022, 1, 3);
        var fom0 = LocalDate.of(2022, 1, 10);
        var tom = LocalDate.of(2022, 6, 10);
        var fom2 = LocalDate.of(2022, 6, 13);
        var tom2 = LocalDate.of(2022, 6, 17);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .build();
        var uttakPeriode1a = new UttakResultatPeriodeEntitet.Builder(fom, fom0.minusDays(1)).medResultatType(PeriodeResultatType.INNVILGET,
                PeriodeResultatÅrsak.UTSETTELSE_GYLDIG_PGA_100_PROSENT_ARBEID)
            .medUtsettelseType(UttakUtsettelseType.ARBEID)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.UDEFINERT)
                .medMorsAktivitet(MorsAktivitet.UTDANNING)
                .build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1a, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(0))
            .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medArbeidsprosent(BigDecimal.TEN.multiply(BigDecimal.TEN))
            .build();
        perioder.leggTilPeriode(uttakPeriode1a);
        var uttakPeriode1b = new UttakResultatPeriodeEntitet.Builder(fom0, tom).medResultatType(PeriodeResultatType.INNVILGET,
                PeriodeResultatÅrsak.UTSETTELSE_GYLDIG_PGA_100_PROSENT_ARBEID)
            .medUtsettelseType(UttakUtsettelseType.ARBEID)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.UDEFINERT)
                .medMorsAktivitet(MorsAktivitet.UTDANNING)
                .build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1b, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(0))
            .medTrekkonto(UttakPeriodeType.UDEFINERT)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medArbeidsprosent(BigDecimal.TEN.multiply(BigDecimal.TEN))
            .build();
        perioder.leggTilPeriode(uttakPeriode1b);

        var uttakPeriode2 = new UttakResultatPeriodeEntitet.Builder(fom2, tom2).medResultatType(PeriodeResultatType.INNVILGET,
                PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .medMorsAktivitet(MorsAktivitet.UTDANNING)
                .build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode2, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(5))
            .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        perioder.leggTilPeriode(uttakPeriode2);
        var uttakResultat = new UttakResultatEntitet.Builder(Behandlingsresultat.builder().build()).medOpprinneligPerioder(perioder).build();

        var søknad0 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.UDEFINERT)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medMorsAktivitet(MorsAktivitet.UTDANNING)
            .build();
        var søknad1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom2, tom2)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMorsAktivitet(MorsAktivitet.UTDANNING)
            .build();
        var søknad2 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(LocalDate.of(2022, 6, 20), LocalDate.of(2022, 8, 19))
            .medPeriodeType(UttakPeriodeType.UDEFINERT)
            .medÅrsak(UtsettelseÅrsak.ARBEID)
            .medMorsAktivitet(MorsAktivitet.UTDANNING)
            .build();

        var filtrert = VedtaksperiodeFilter.filtrerVekkPerioderSomErLikeInnvilgetUttak(123L, List.of(søknad0, søknad1, søknad2), uttakResultat,
            false);
        assertThat(filtrert).hasSize(1);
        assertThat(filtrert.stream().anyMatch(p -> p.equals(søknad2))).isTrue();
    }

    @Test
    void skalHåndtereAtUttaksresultatInneholderPerioderSomBareErInnvilgetHelger() {
        var lørdag = LocalDate.of(2022, 10, 29);
        var søndag = LocalDate.of(2022, 10, 30);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var aktivitet = new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .build();
        var helg1 = uttaksperiodeMedForeldrepenger(lørdag, søndag, aktivitet, Trekkdager.ZERO);
        var likUttaksperiode = uttaksperiodeMedForeldrepenger(søndag.plusDays(1), søndag.plusWeeks(2).minusDays(2), aktivitet, new Trekkdager(10));
        var helg2 = uttaksperiodeMedForeldrepenger(søndag.plusWeeks(2), søndag.plusWeeks(2), aktivitet, Trekkdager.ZERO);
        var forskjellUttaksperiode = uttaksperiodeMedForeldrepenger(søndag.plusWeeks(2).plusDays(1), søndag.plusWeeks(4), aktivitet,
            new Trekkdager(10));
        perioder.leggTilPeriode(helg1);
        perioder.leggTilPeriode(likUttaksperiode);
        perioder.leggTilPeriode(helg2);
        perioder.leggTilPeriode(forskjellUttaksperiode);
        var uttakResultat = new UttakResultatEntitet.Builder(Behandlingsresultat.builder().build()).medOpprinneligPerioder(perioder).build();

        // Lik eksisterende vedtak
        var søknad1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(lørdag.plusDays(2), helg2.getTom())
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        // Ny periode
        var søknad2 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(forskjellUttaksperiode.getFom(), forskjellUttaksperiode.getTom())
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(new SamtidigUttaksprosent(50))
            .build();

        var filtrert = VedtaksperiodeFilter.filtrerVekkPerioderSomErLikeInnvilgetUttak(123L, List.of(søknad1, søknad2), uttakResultat, false);
        assertThat(filtrert).hasSize(1);
        assertThat(filtrert.get(0)).isEqualTo(søknad2);
    }

    private static UttakResultatPeriodeEntitet uttaksperiodeMedForeldrepenger(LocalDate fom,
                                                                              LocalDate tom,
                                                                              UttakAktivitetEntitet arbeidsforhold,
                                                                              Trekkdager trekkdager) {
        var periode = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER).build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(periode, arbeidsforhold)
            .medTrekkdager(trekkdager)
            .medTrekkonto(UttakPeriodeType.FORELDREPENGER)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        return periode;
    }

}
