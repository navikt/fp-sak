package no.nav.foreldrepenger.domene.uttak.saldo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.KontoerGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.RettOgOmsorgGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Orgnummer;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;

public class StønadskontoSaldoTjenesteTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    @Test
    public void skal_regne_ut_saldo_per_aktivitet() {
        var tjeneste = tjeneste();
        var behandling = behandlingMedKonto(
            Stønadskonto.builder().medMaxDager(15).medStønadskontoType(StønadskontoType.FELLESPERIODE).build());

        var uttak = new UttakResultatPerioderEntitet();
        var uttaksperiode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(), LocalDate.now()).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
        var aktivitet1 = new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build()).medTrekkdager(
            new Trekkdager(10)).medTrekkonto(StønadskontoType.FELLESPERIODE).medArbeidsprosent(BigDecimal.TEN).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var aktivitet2 = new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .medArbeidsforhold(arbeidsgiver, null)
                .build()).medTrekkdager(new Trekkdager(3))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        uttak.leggTilPeriode(uttaksperiode);
        uttaksperiode.leggTilAktivitet(aktivitet1);
        uttaksperiode.leggTilAktivitet(aktivitet2);
        lagreUttak(uttak, behandling.getId());

        var saldoUtregning = tjeneste.finnSaldoUtregning(input(behandling));

        assertThat(saldoUtregning.aktiviteterForSøker()).hasSize(2);
        assertThat(saldoUtregning.getMaxDager(Stønadskontotype.FELLESPERIODE)).isEqualTo(15);
        assertThat(saldoUtregning.stønadskontoer()).hasSize(1);
        assertThat(saldoUtregning.saldo(Stønadskontotype.FELLESPERIODE)).isEqualTo(12);
        assertThat(saldoUtregning.saldo(Stønadskontotype.FELLESPERIODE, AktivitetIdentifikator.forFrilans())).isEqualTo(
            5);
        var orgnummer = new Orgnummer(arbeidsgiver.getIdentifikator());
        assertThat(saldoUtregning.saldo(Stønadskontotype.FELLESPERIODE,
            AktivitetIdentifikator.forArbeid(orgnummer, null))).isEqualTo(12);
    }

    @Test
    public void skal_regne_ut_for_arbeidstaker_uten_arbeidsgiver() {
        var tjeneste = tjeneste();
        var behandling = behandlingMedKonto(
            Stønadskonto.builder().medMaxDager(15).medStønadskontoType(StønadskontoType.FELLESPERIODE).build());

        var uttak = new UttakResultatPerioderEntitet();
        var uttaksperiode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(), LocalDate.now()).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
        var aktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .medArbeidsforhold(null, null)
                .build()).medTrekkdager(new Trekkdager(10))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        uttak.leggTilPeriode(uttaksperiode);
        uttaksperiode.leggTilAktivitet(aktivitet);
        lagreUttak(uttak, behandling.getId());

        var saldoUtregning = tjeneste.finnSaldoUtregning(input(behandling));

        assertThat(saldoUtregning.saldo(Stønadskontotype.FELLESPERIODE)).isEqualTo(5);
    }

    @Test
    public void skal_gi_riktig_konto_for_arbeidsforhold_som_starter_i_løpet_av_uttaket() {
        var enOnsdag = LocalDate.of(2019, 12, 18);
        var tjeneste = tjeneste();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var arbeidsforholdRef1 = InternArbeidsforholdRef.nyRef();
        var arbeidsforholdRef2 = InternArbeidsforholdRef.nyRef();
        var aktørId = AktørId.dummy();
        var stønadskonto = Stønadskonto.builder()
            .medMaxDager(15)
            .medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .build();
        var behandling = behandlingMedKonto(stønadskonto, aktørId);

        //Periode med bare frilans
        var uttak = new UttakResultatPerioderEntitet();
        var uttaksperiode1 = new UttakResultatPeriodeEntitet.Builder(enOnsdag, enOnsdag).medResultatType(
            PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
        var uttakFrilansAktivitet = new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();
        var frilansAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode1,
            uttakFrilansAktivitet).medTrekkdager(new Trekkdager(3))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttaksperiode1.leggTilAktivitet(frilansAktivitet);
        uttak.leggTilPeriode(uttaksperiode1);

        //Periode med arbeidsgiver + frilans
        var uttaksperiode2 = new UttakResultatPeriodeEntitet.Builder(enOnsdag.plusDays(1),
            enOnsdag.plusDays(1)).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
        var uttakAktivitetArbeidsgiver1 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(
            UttakArbeidType.ORDINÆRT_ARBEID).medArbeidsforhold(arbeidsgiver, arbeidsforholdRef1).build();
        var arbeidsgiverAktivitet1 = new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode2,
            uttakAktivitetArbeidsgiver1).medTrekkdager(new Trekkdager(3))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        var frilansAktivitet2 = new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode2,
            uttakFrilansAktivitet).medTrekkdager(new Trekkdager(3))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(uttaksperiode2);
        uttaksperiode2.leggTilAktivitet(frilansAktivitet2);
        uttaksperiode2.leggTilAktivitet(arbeidsgiverAktivitet1);

        //Periode med arbeidsgiver med 2 arbeidsforhold + frilans
        var uttaksperiode3 = new UttakResultatPeriodeEntitet.Builder(enOnsdag.plusDays(2),
            enOnsdag.plusDays(2)).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
        var uttakAktivitetArbeidsgiver2 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(
            UttakArbeidType.ORDINÆRT_ARBEID).medArbeidsforhold(arbeidsgiver, arbeidsforholdRef2).build();
        var arbeidsgiverAktivitet2 = new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode3,
            uttakAktivitetArbeidsgiver2).medTrekkdager(new Trekkdager(3))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        var arbeidsgiverAktivitet3 = new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode3,
            uttakAktivitetArbeidsgiver1).medTrekkdager(new Trekkdager(3))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        var frilansAktivitet3 = new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode3,
            uttakFrilansAktivitet).medTrekkdager(new Trekkdager(3))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(uttaksperiode3);
        uttaksperiode3.leggTilAktivitet(frilansAktivitet3);
        uttaksperiode3.leggTilAktivitet(arbeidsgiverAktivitet2);
        uttaksperiode3.leggTilAktivitet(arbeidsgiverAktivitet3);

        lagreUttak(uttak, behandling.getId());

        var saldoUtregning = tjeneste.finnSaldoUtregning(input(behandling));

        var aktivitet1 = AktivitetIdentifikator.forArbeid(
            new Orgnummer(uttakAktivitetArbeidsgiver1.getArbeidsgiver().get().getIdentifikator()),
            uttakAktivitetArbeidsgiver1.getArbeidsforholdRef().getReferanse());
        var aktivitet2 = AktivitetIdentifikator.forArbeid(
            new Orgnummer(uttakAktivitetArbeidsgiver2.getArbeidsgiver().get().getIdentifikator()),
            uttakAktivitetArbeidsgiver2.getArbeidsforholdRef().getReferanse());

        assertThat(saldoUtregning.saldo(Stønadskontotype.FELLESPERIODE, AktivitetIdentifikator.forFrilans())).isEqualTo(
            6);
        assertThat(saldoUtregning.saldo(Stønadskontotype.FELLESPERIODE, aktivitet1)).isEqualTo(6);
        assertThat(saldoUtregning.saldo(Stønadskontotype.FELLESPERIODE, aktivitet2)).isEqualTo(6);
    }

    private UttakInput input(Behandling behandling) {
        var foreldrepengerGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(FamilieHendelse.forFødsel(null, LocalDate.MIN,
                List.of(), 1)));
        return new UttakInput(BehandlingReferanse.fra(behandling), null, foreldrepengerGrunnlag);
    }

    @Test
    public void skal_returnere_alle_aktiviteter_hvis_ingen_trekkdager() {
        var tjeneste = tjeneste();
        var behandling = behandlingMedKonto(
            Stønadskonto.builder().medMaxDager(15).medStønadskontoType(StønadskontoType.FELLESPERIODE).build());

        var uttak = new UttakResultatPerioderEntitet();
        var uttaksperiode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(), LocalDate.now()).medResultatType(
            PeriodeResultatType.MANUELL_BEHANDLING, PeriodeResultatÅrsak.UKJENT).build();
        var aktivitet1 = new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build()).medTrekkdager(
            Trekkdager.ZERO).medTrekkonto(StønadskontoType.FELLESPERIODE).medArbeidsprosent(BigDecimal.TEN).build();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var aktivitet2 = new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .medArbeidsforhold(arbeidsgiver, null)
                .build()).medTrekkdager(Trekkdager.ZERO)
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        uttak.leggTilPeriode(uttaksperiode);
        uttaksperiode.leggTilAktivitet(aktivitet1);
        uttaksperiode.leggTilAktivitet(aktivitet2);
        lagreUttak(uttak, behandling.getId());

        var saldoUtregning = tjeneste.finnSaldoUtregning(input(behandling));

        assertThat(saldoUtregning.aktiviteterForSøker()).hasSize(2);
        assertThat(saldoUtregning.getMaxDager(Stønadskontotype.FELLESPERIODE)).isEqualTo(15);
        assertThat(saldoUtregning.stønadskontoer()).hasSize(1);
        assertThat(saldoUtregning.saldo(Stønadskontotype.FELLESPERIODE)).isEqualTo(15);
        assertThat(saldoUtregning.saldo(Stønadskontotype.FELLESPERIODE, AktivitetIdentifikator.forFrilans())).isEqualTo(
            15);
        assertThat(saldoUtregning.saldo(Stønadskontotype.FELLESPERIODE,
            AktivitetIdentifikator.forArbeid(new Orgnummer(arbeidsgiver.getIdentifikator()), null))).isEqualTo(15);
    }

    private void lagreUttak(UttakResultatPerioderEntitet uttak, Long behandlingId) {
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandlingId, uttak);
    }

    private Behandling behandlingMedKonto(Stønadskonto konto) {
        return behandlingMedKonto(konto, AktørId.dummy());
    }

    private Behandling behandlingMedKonto(Stønadskonto konto, AktørId aktørId) {
        var søknadsperioder = Collections.singletonList(OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.of(2019, 2, 2), LocalDate.of(2019, 10, 2))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build());
        var fordeling = new OppgittFordelingEntitet(søknadsperioder, true);
        var behandling = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId)
            .medFordeling(fordeling)
            .medOppgittRettighet(new OppgittRettighetEntitet(true, false, false))
            .medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100())
            .lagre(repositoryProvider);

        var kontoer = Stønadskontoberegning.builder()
            .medStønadskonto(konto)
            .medRegelEvaluering("asd")
            .medRegelInput("awd")
            .build();
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), behandling.getId(), kontoer);
        return behandling;
    }

    private StønadskontoSaldoTjeneste tjeneste() {
        return new StønadskontoSaldoTjeneste(repositoryProvider, new KontoerGrunnlagBygger(repositoryProvider,
            new RettOgOmsorgGrunnlagBygger(repositoryProvider, new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()))));
    }

}
