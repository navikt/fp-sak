package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
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

@ExtendWith(MockitoExtension.class)
public class DokVurderingKopiererTest {

    @Test
    public void skalKopiereVurderingVedLikePerioder() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriode(fom, tom)
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .medMorsAktivitet(MorsAktivitet.UTDANNING)
                .medDokumentasjonVurdering(DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT)
                .build());

        var periode = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMorsAktivitet(MorsAktivitet.UTDANNING)
            .build();

        var tidligerefordeling = new OppgittFordelingEntitet(originalBehandlingPerioder, true, false);
        var oppdatert = DokVurderingKopierer.oppdaterMedDokumentasjonVurdering(List.of(periode), List.of(tidligerefordeling), Optional.empty());
        assertThat(oppdatert).hasSize(1);
        var oppdatertperiode = oppdatert.get(0);
        assertThat(oppdatertperiode.getDokumentasjonVurdering()).isEqualTo(DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT);
        assertThat(oppdatertperiode.getFom()).isEqualTo(periode.getFom());
        assertThat(oppdatertperiode.getTom()).isEqualTo(periode.getTom());
    }


    @Test
    public void skalIkkeKopiereVurderingVedIkkeGodkjent() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_BARN)
            .medDokumentasjonVurdering(DokumentasjonVurdering.INNLEGGELSE_BARN_IKKE_GODKJENT)
            .build());

        var periode = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_BARN)
            .build();

        var tidligerefordeling = new OppgittFordelingEntitet(originalBehandlingPerioder, true, false);
        var oppdatert = DokVurderingKopierer.oppdaterMedDokumentasjonVurdering(List.of(periode), List.of(tidligerefordeling), Optional.empty());
        assertThat(oppdatert).hasSize(1);
        var oppdatertperiode = oppdatert.get(0);
        assertThat(oppdatertperiode.getDokumentasjonVurdering()).isNull();
        assertThat(oppdatertperiode.getFom()).isEqualTo(periode.getFom());
        assertThat(oppdatertperiode.getTom()).isEqualTo(periode.getTom());
    }

    @Test
    public void skalIkkeKopiereVurderingVedAnnenOverføringsårsak() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medÅrsak(OverføringÅrsak.SYKDOM_ANNEN_FORELDER)
            .medDokumentasjonVurdering(DokumentasjonVurdering.SYKDOM_ANNEN_FORELDER_GODKJENT)
            .build());

        var periode = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medÅrsak(OverføringÅrsak.ALENEOMSORG)
            .build();

        var tidligerefordeling = new OppgittFordelingEntitet(originalBehandlingPerioder, true, false);
        var oppdatert = DokVurderingKopierer.oppdaterMedDokumentasjonVurdering(List.of(periode), List.of(tidligerefordeling), Optional.empty());
        assertThat(oppdatert).hasSize(1);
        var oppdatertperiode = oppdatert.get(0);
        assertThat(oppdatertperiode.getDokumentasjonVurdering()).isNull();
        assertThat(oppdatertperiode.getFom()).isEqualTo(periode.getFom());
        assertThat(oppdatertperiode.getTom()).isEqualTo(periode.getTom());
    }


    @Test
    public void skalSplittePeriodeFinneVurderingFraOriginalBehandlingHvisUtvidetPeriode() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMorsAktivitet(MorsAktivitet.UTDANNING)
            .medDokumentasjonVurdering(DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT)
            .build());

        var periode = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom.plusWeeks(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMorsAktivitet(MorsAktivitet.UTDANNING)
            .build();

        var tidligerefordeling = new OppgittFordelingEntitet(originalBehandlingPerioder, true, false);
        var oppdatert = DokVurderingKopierer.oppdaterMedDokumentasjonVurdering(List.of(periode), List.of(tidligerefordeling), Optional.empty());

        assertThat(oppdatert).hasSize(2);
        assertThat(oppdatert.get(0).getDokumentasjonVurdering()).isEqualTo(DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT);
        assertThat(oppdatert.get(1).getDokumentasjonVurdering()).isNull();
        assertThat(oppdatert.get(0).getFom()).isEqualTo(periode.getFom());
        assertThat(oppdatert.get(0).getTom()).isEqualTo(tom);
        assertThat(oppdatert.get(1).getFom()).isEqualTo(tom.plusDays(1));
        assertThat(oppdatert.get(1).getTom()).isEqualTo(periode.getTom());

    }

    @Test
    public void skalFinneVurderingOriginalBehandlingHvisKrympetPeriode() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_BARN)
            .medDokumentasjonVurdering(DokumentasjonVurdering.INNLEGGELSE_BARN_GODKJENT)
            .build());

        var periode = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom.minusWeeks(1))
            .medÅrsak(UtsettelseÅrsak.INSTITUSJON_BARN)
            .build();

        var tidligerefordeling = new OppgittFordelingEntitet(originalBehandlingPerioder, true, false);
        var oppdatert = DokVurderingKopierer.oppdaterMedDokumentasjonVurdering(List.of(periode), List.of(tidligerefordeling), Optional.empty());

        assertThat(oppdatert).hasSize(1);
        var oppdatertperiode = oppdatert.get(0);
        assertThat(oppdatert.get(0).getDokumentasjonVurdering()).isEqualTo(DokumentasjonVurdering.INNLEGGELSE_BARN_GODKJENT);
        assertThat(oppdatertperiode.getFom()).isEqualTo(periode.getFom());
        assertThat(oppdatertperiode.getTom()).isEqualTo(periode.getTom());
    }


    @Test
    public void skalBrukeVurderingSelvOmEndretGradering() {
        var fom = LocalDate.of(2022, 10, 10);
        var tom = LocalDate.of(2022, 11, 9);

        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsprosent(BigDecimal.TEN)
            .medMorsAktivitet(MorsAktivitet.TRENGER_HJELP)
            .medDokumentasjonVurdering(DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT)
            .build());

        // Lik eksisterende vedtak
        var søknad1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsprosent(BigDecimal.TEN.multiply(BigDecimal.valueOf(2)))
            .medMorsAktivitet(MorsAktivitet.TRENGER_HJELP)
            .build();
        // Ny periode
        var søknad2 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(tom.plusDays(1), tom.plusWeeks(4))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMorsAktivitet(MorsAktivitet.TRENGER_HJELP)
            .build();

        var tidligerefordeling = new OppgittFordelingEntitet(originalBehandlingPerioder, true, false);
        var oppdatert = DokVurderingKopierer.oppdaterMedDokumentasjonVurdering(List.of(søknad1, søknad2), List.of(tidligerefordeling), Optional.empty());

        assertThat(oppdatert).hasSize(2);
        assertThat(oppdatert.get(0).getDokumentasjonVurdering()).isEqualTo(DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT);
        assertThat(oppdatert.get(1).getDokumentasjonVurdering()).isNull();
        assertThat(oppdatert.get(0).getFom()).isEqualTo(søknad1.getFom());
        assertThat(oppdatert.get(0).getTom()).isEqualTo(søknad1.getTom());
        assertThat(oppdatert.get(1).getFom()).isEqualTo(søknad2.getFom());
        assertThat(oppdatert.get(1).getTom()).isEqualTo(søknad2.getTom());
    }

    @Test
    public void utviderPeriodeUtenUttakOgLeggerTilSenereUttakFraUR() {
        var fom0 = LocalDate.of(2022, 10, 4);
        var fom = LocalDate.of(2022, 10, 10);
        var tom = LocalDate.of(2022, 11, 9);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .build();
        var uttakPeriode0 = new UttakResultatPeriodeEntitet.Builder(fom0, fom0.plusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .medMorsAktivitet(MorsAktivitet.UTDANNING)
                .medDokumentasjonVurdering(DokumentasjonVurdering.MORS_AKTIVITET_IKKE_DOKUMENTERT)
                .build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode0, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(42))
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .medArbeidsprosent(BigDecimal.ZERO).build();
        perioder.leggTilPeriode(uttakPeriode0);
        var uttakPeriode1 = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder()
                .medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .medMorsAktivitet(MorsAktivitet.UTDANNING)
                .medDokumentasjonVurdering(DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT)
                .build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(42))
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .medArbeidsprosent(BigDecimal.ZERO).build();
        perioder.leggTilPeriode(uttakPeriode1);
        var uttakResultat = new UttakResultatEntitet.Builder(Behandlingsresultat.builder().build()).medOpprinneligPerioder(perioder).build();

        // Utvider oppholdet mellom uttaket med et par dager
        var søknad0 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom0, fom0.plusDays(1))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMorsAktivitet(MorsAktivitet.UTDANNING)
            .build();
        var søknad1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom.plusDays(3), tom.plusWeeks(2))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medMorsAktivitet(MorsAktivitet.UTDANNING)
            .build();

        var oppdatert = DokVurderingKopierer.oppdaterMedDokumentasjonVurdering(List.of(søknad0, søknad1), List.of(), Optional.of(uttakResultat));

        assertThat(oppdatert).hasSize(3);
        assertThat(oppdatert.get(0).getDokumentasjonVurdering()).isNull();
        assertThat(oppdatert.get(1).getDokumentasjonVurdering()).isEqualTo(DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT);
        assertThat(oppdatert.get(2).getDokumentasjonVurdering()).isNull();
        assertThat(oppdatert.get(0).getFom()).isEqualTo(søknad0.getFom());
        assertThat(oppdatert.get(0).getTom()).isEqualTo(søknad0.getTom());
        assertThat(oppdatert.get(1).getFom()).isEqualTo(søknad1.getFom());
        assertThat(oppdatert.get(1).getTom()).isEqualTo(tom);
        assertThat(oppdatert.get(2).getFom()).isEqualTo(tom.plusDays(1));
        assertThat(oppdatert.get(2).getTom()).isEqualTo(søknad1.getTom());
    }

    @Test
    public void prod_overstyrt_uttak_utsettelse_med_konto() {
        var fom = LocalDate.of(2022, 1, 3);
        var fom0 = LocalDate.of(2022, 1, 10);
        var tom = LocalDate.of(2022, 6, 10);
        var fom2 = LocalDate.of(2022, 6, 13);
        var tom2 = LocalDate.of(2022, 6, 17);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .build();
        var uttakPeriode1a = new UttakResultatPeriodeEntitet.Builder(fom, fom0.minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UTSETTELSE_GYLDIG_PGA_100_PROSENT_ARBEID)
            .medUtsettelseType(UttakUtsettelseType.ARBEID)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder()
                .medUttakPeriodeType(UttakPeriodeType.UDEFINERT)
                .medMorsAktivitet(MorsAktivitet.UTDANNING)
                .medDokumentasjonVurdering(DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT)
                .build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1a, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(0))
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medArbeidsprosent(BigDecimal.TEN.multiply(BigDecimal.TEN)).build();
        perioder.leggTilPeriode(uttakPeriode1a);
        var uttakPeriode1b = new UttakResultatPeriodeEntitet.Builder(fom0, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UTSETTELSE_GYLDIG_PGA_100_PROSENT_ARBEID)
            .medUtsettelseType(UttakUtsettelseType.ARBEID)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder()
                .medUttakPeriodeType(UttakPeriodeType.UDEFINERT)
                .medMorsAktivitet(MorsAktivitet.UTDANNING)
                .medDokumentasjonVurdering(DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT)
                .build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1b, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(0))
            .medTrekkonto(StønadskontoType.UDEFINERT)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medArbeidsprosent(BigDecimal.TEN.multiply(BigDecimal.TEN)).build();
        perioder.leggTilPeriode(uttakPeriode1b);

        var uttakPeriode2 = new UttakResultatPeriodeEntitet.Builder(fom2, tom2)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder()
                .medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .medUttakPeriodeType(UttakPeriodeType.UDEFINERT)
                .medMorsAktivitet(MorsAktivitet.UTDANNING)
                .medDokumentasjonVurdering(DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT)
                .build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode2, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(5))
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medArbeidsprosent(BigDecimal.ZERO).build();
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

        var oppdatert = DokVurderingKopierer.oppdaterMedDokumentasjonVurdering(List.of(søknad0, søknad1, søknad2), List.of(), Optional.of(uttakResultat));

        assertThat(oppdatert).hasSize(3);
        assertThat(oppdatert.get(0).getDokumentasjonVurdering()).isEqualTo(DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT);
        assertThat(oppdatert.get(1).getDokumentasjonVurdering()).isEqualTo(DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT);
        assertThat(oppdatert.get(2).getDokumentasjonVurdering()).isNull();
        assertThat(oppdatert.get(0).getFom()).isEqualTo(søknad0.getFom());
        assertThat(oppdatert.get(0).getTom()).isEqualTo(søknad0.getTom());
        assertThat(oppdatert.get(1).getFom()).isEqualTo(søknad1.getFom());
        assertThat(oppdatert.get(1).getTom()).isEqualTo(søknad1.getTom());
        assertThat(oppdatert.get(2).getFom()).isEqualTo(søknad2.getFom());
        assertThat(oppdatert.get(2).getTom()).isEqualTo(søknad2.getTom());
    }

}
