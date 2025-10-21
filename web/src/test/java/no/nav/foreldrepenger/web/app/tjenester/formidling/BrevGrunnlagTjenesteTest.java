package no.nav.foreldrepenger.web.app.tjenester.formidling;

import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.FagsakYtelseType;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.Foreldrepenger;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.Rettigheter;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.MorsStillingsprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;


@CdiDbAwareTest
class BrevGrunnlagTjenesteTest {

    @Inject
    private BrevGrunnlagTjeneste brevGrunnlagTjeneste;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    void foreldrepenger() {
        var fødselsdato = LocalDate.now();
        var oppgittRettighet = OppgittRettighetEntitet.beggeRett();
        var behandling = opprettAvsluttetFpBehandling(fødselsdato, oppgittRettighet);

        var uttak = new UttakResultatPerioderEntitet();
        var fom = LocalDate.of(2023, 3, 5);
        var tom = LocalDate.of(2023, 10, 5);
        var periode = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE).build();
        uttak.leggTilPeriode(periode);
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        var uttakAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .medArbeidsforhold(arbeidsgiver, arbeidsforholdRef)
                .build()).medArbeidsprosent(BigDecimal.TEN)
            .medTrekkdager(new Trekkdager(10))
            .medErSøktGradering(true)
            .medUtbetalingsgrad(new Utbetalingsgrad(90))
            .build();
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttak);


        var brevGrunnlagDto = brevGrunnlagTjeneste.lagGrunnlagDto(behandling);

        assertThat(brevGrunnlagDto.fagsakYtelseType()).isEqualTo(FagsakYtelseType.FORELDREPENGER);

        assertThat(brevGrunnlagDto.saksnummer()).isEqualTo(behandling.getSaksnummer().getVerdi());
        assertThat(brevGrunnlagDto.aktørId()).isEqualTo(behandling.getAktørId().getId());
        assertThat(brevGrunnlagDto.behandlendeEnhet()).isEqualTo(behandling.getBehandlendeEnhet());
        assertThat(brevGrunnlagDto.foreldrepenger()).isNotNull();
        assertThat(brevGrunnlagDto.foreldrepenger().dekningsgrad()).isEqualTo(BrevGrunnlagDto.Dekningsgrad.ÅTTI);
        assertThat(brevGrunnlagDto.foreldrepenger().perioderSøker()).hasSize(1);
        assertThat(brevGrunnlagDto.foreldrepenger().perioderSøker().getFirst().fom()).isEqualTo(periode.getFom());
        assertThat(brevGrunnlagDto.foreldrepenger().perioderSøker().getFirst().periodeResultatType()).isEqualTo(
            BrevGrunnlagDto.PeriodeResultatType.INNVILGET);
        assertThat(brevGrunnlagDto.foreldrepenger().perioderSøker().getFirst().aktiviteter().getFirst().arbeidsgiverReferanse()).isEqualTo(
            arbeidsgiver.getIdentifikator());
        assertThat(brevGrunnlagDto.foreldrepenger().perioderSøker().getFirst().aktiviteter().getFirst().utbetalingsgrad()).isEqualTo(
            uttakAktivitet.getUtbetalingsgrad().decimalValue());
        assertThat(brevGrunnlagDto.foreldrepenger().rettigheter().gjeldende()).isEqualTo(Rettigheter.Rettighetstype.BEGGE_RETT);
    }

    @Test
    void engangsstønad() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var brevGrunnlagDto = brevGrunnlagTjeneste.lagGrunnlagDto(behandling);

        assertThat(brevGrunnlagDto.fagsakYtelseType()).isEqualTo(FagsakYtelseType.ENGANGSTØNAD);
    }

    private Behandling opprettAvsluttetFpBehandling(LocalDate fødselsdato, OppgittRettighetEntitet oppgittRettighet) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(oppgittRettighet)
            .medFødselAdopsjonsdato(fødselsdato)
            .medDefaultFordeling(fødselsdato.minusWeeks(3))
            .medOppgittDekningsgrad(Dekningsgrad._80);

        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenario.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET))
            .lagre(repositoryProvider);

        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));
        return behandling;
    }

    @Test
    void skal_sette_erUtbetalingRedusertTilMorsStillingsprosent_til_true_når_aktivitetskrav_og_utbetaling_er_redusert_med_mors_stillingsprosent() {
        var perioder = new UttakResultatPerioderEntitet();
        var periodeType = UttakPeriodeType.FORELDREPENGER;
        var mottattDato = LocalDate.now();
        var internArbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var uttakAktivitet = new UttakAktivitetEntitet.Builder().medArbeidsforhold(arbeidsgiver, internArbeidsforholdId)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        var periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(periodeType)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .medDokumentasjonVurdering(
                new DokumentasjonVurdering(DokumentasjonVurdering.Type.MORS_AKTIVITET_GODKJENT, new MorsStillingsprosent(BigDecimal.valueOf(55))))
            .medMottattDato(mottattDato)
            .build();
        var periode = periodeBuilder(LocalDate.now(), LocalDate.now().plusWeeks(2)).medGraderingInnvilget(false)
            .medSamtidigUttak(false)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(periodeSøknad)
            .build();
        var periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet).medTrekkonto(UttakPeriodeType.FORELDREPENGER)
            .medErSøktGradering(false)
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(new Utbetalingsgrad(55))
            .build();
        periode.leggTilAktivitet(periodeAktivitet);
        perioder.leggTilPeriode(periode);


        var behandling = morBehandlingMedUttak(perioder);

        var result = brevGrunnlagTjeneste.lagGrunnlagDto(behandling);
        assertThat(result.foreldrepenger()
            .perioderSøker()
            .stream()
            .anyMatch(Foreldrepenger.Uttaksperiode::erUtbetalingRedusertTilMorsStillingsprosent)).isTrue();
    }

    @Test
    void skal_sette_erUtbetalingRedusertTilMorsStillingsprosent_til_false_når_utbetalingen_erRedusert_men_innvilget_gradering() {
        var perioder = new UttakResultatPerioderEntitet();
        var periodeType = UttakPeriodeType.FORELDREPENGER;
        var mottattDato = LocalDate.now();
        var internArbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var uttakAktivitet = new UttakAktivitetEntitet.Builder().medArbeidsforhold(arbeidsgiver, internArbeidsforholdId)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        var periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(periodeType)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .medDokumentasjonVurdering(
                new DokumentasjonVurdering(DokumentasjonVurdering.Type.MORS_AKTIVITET_GODKJENT, new MorsStillingsprosent(BigDecimal.valueOf(40))))
            .medMottattDato(mottattDato)
            .build();
        var periode = periodeBuilder(LocalDate.now(), LocalDate.now().plusWeeks(2)).medGraderingInnvilget(true)
            .medSamtidigUttak(false)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(periodeSøknad)
            .build();
        var periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet).medTrekkonto(UttakPeriodeType.FORELDREPENGER)
            .medErSøktGradering(true)
            .medArbeidsprosent(BigDecimal.valueOf(60))
            .medUtbetalingsgrad(new Utbetalingsgrad(40))
            .build();
        periode.leggTilAktivitet(periodeAktivitet);
        perioder.leggTilPeriode(periode);

        var behandling = morBehandlingMedUttak(perioder);

        var result = brevGrunnlagTjeneste.lagGrunnlagDto(behandling);
        assertThat(result.foreldrepenger()
            .perioderSøker()
            .stream()
            .anyMatch(Foreldrepenger.Uttaksperiode::erUtbetalingRedusertTilMorsStillingsprosent)).isFalse();
    }

    private UttakResultatPeriodeEntitet.Builder periodeBuilder(LocalDate fom, LocalDate tom) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT);
    }

    private Behandling morBehandlingMedUttak(UttakResultatPerioderEntitet perioder) {
        return morBehandlingMedUttak(perioder, LocalDateTime.now());
    }

    private Behandling morBehandlingMedUttak(UttakResultatPerioderEntitet perioder, LocalDateTime vedtakstidspunkt) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        return behandlingMedUttak(perioder, scenario, vedtakstidspunkt);
    }

    private Behandling behandlingMedUttak(UttakResultatPerioderEntitet perioder, AbstractTestScenario<?> scenario, LocalDateTime vedtakstidspunkt) {
        scenario.medUttak(perioder);
        scenario.medDefaultOppgittDekningsgrad();
        scenario.medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        scenario.medBehandlingVedtak().medVedtakstidspunkt(vedtakstidspunkt);
        scenario.medFødselAdopsjonsdato(perioder.getPerioder().getFirst().getFom());
        scenario.medFordeling(new OppgittFordelingEntitet(List.of(), true));
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        return behandling;
    }
}
