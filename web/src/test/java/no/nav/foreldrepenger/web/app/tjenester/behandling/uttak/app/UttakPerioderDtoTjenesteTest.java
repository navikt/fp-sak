package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;

public class UttakPerioderDtoTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;

    private final String orgnr = UUID.randomUUID().toString();
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private AbakusInMemoryInntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        uttakTjeneste = new ForeldrepengerUttakTjeneste(new FpUttakRepository(entityManager));
    }

    @Test
    public void skalHenteUttaksPerioderFraRepository() {
        var perioder = new UttakResultatPerioderEntitet();
        var internArbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var eksternArbeidsforholdId = EksternArbeidsforholdRef.ref("ID1");
        var arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
        var uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(arbeidsgiver, internArbeidsforholdId)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        var periodeType = UttakPeriodeType.MØDREKVOTE;
        var mottattDato = LocalDate.now();
        var periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medUttakPeriodeType(periodeType)
            .medMottattDato(mottattDato)
            .build();
        var periode = periodeBuilder(LocalDate.now(), LocalDate.now().plusWeeks(2))
            .medGraderingInnvilget(true)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
            .medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(periodeSøknad)
            .build();
        var periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet)
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.TEN)
            .medErSøktGradering(true)
            .medUtbetalingsgrad(new Utbetalingsgrad(1))
            .build();
        perioder.leggTilPeriode(periode);

        var behandling = morBehandlingMedUttak(perioder);

        var arbeidsforholdInformasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());
        arbeidsforholdInformasjonBuilder.leggTil(arbeidsgiver, internArbeidsforholdId, eksternArbeidsforholdId);
        inntektArbeidYtelseTjeneste.lagreArbeidsforhold(behandling.getId(), behandling.getAktørId(), arbeidsforholdInformasjonBuilder);

        var tjeneste = tjeneste();

        var result = tjeneste.mapFra(behandling);

        assertThat(result).isPresent();
        assertThat(result.get().perioderSøker()).hasSize(1);
        assertThat(result.get().perioderSøker().get(0).getFom()).isEqualTo(periode.getFom());
        assertThat(result.get().perioderSøker().get(0).getTom()).isEqualTo(periode.getTom());
        assertThat(result.get().perioderSøker().get(0).isSamtidigUttak()).isEqualTo(periode.isSamtidigUttak());
        assertThat(result.get().perioderSøker().get(0).getPeriodeResultatType()).isEqualTo(periode.getResultatType());
        assertThat(result.get().perioderSøker().get(0).getBegrunnelse()).isEqualTo(periode.getBegrunnelse());
        assertThat(result.get().perioderSøker().get(0).getGradertAktivitet().getArbeidsforholdId()).isEqualTo(internArbeidsforholdId.getReferanse());
        assertThat(result.get().perioderSøker().get(0).getGradertAktivitet().getEksternArbeidsforholdId()).isEqualTo(eksternArbeidsforholdId.getReferanse());
        assertThat(result.get().perioderSøker().get(0).isSamtidigUttak()).isEqualTo(periode.isSamtidigUttak());
        assertThat(result.get().perioderSøker().get(0).getSamtidigUttaksprosent()).isEqualTo(periode.getSamtidigUttaksprosent());
        assertThat(result.get().perioderSøker().get(0).getPeriodeType()).isEqualTo(periodeType);
        assertThat(result.get().perioderSøker().get(0).getMottattDato()).isEqualTo(mottattDato);
        assertThat(result.get().perioderSøker().get(0).getAktiviteter()).hasSize(1);
        assertThat(result.get().perioderSøker().get(0).getAktiviteter().get(0).getArbeidsforholdId()).isEqualTo(periodeAktivitet.getArbeidsforholdRef().getReferanse());
        assertThat(result.get().perioderSøker().get(0).getAktiviteter().get(0).getEksternArbeidsforholdId()).isEqualTo(eksternArbeidsforholdId.getReferanse());
        assertThat(result.get().perioderSøker().get(0).getAktiviteter().get(0).getArbeidsgiverReferanse()).isEqualTo(periodeAktivitet.getArbeidsgiver().getIdentifikator());
        assertThat(result.get().perioderSøker().get(0).getAktiviteter().get(0).getStønadskontoType()).isEqualTo(periodeAktivitet.getTrekkonto());
        assertThat(result.get().perioderSøker().get(0).getAktiviteter().get(0).getTrekkdager()).isEqualTo(periodeAktivitet.getTrekkdager().decimalValue());
        assertThat(result.get().perioderSøker().get(0).getAktiviteter().get(0).getProsentArbeid()).isEqualTo(periodeAktivitet.getArbeidsprosent());
        assertThat(result.get().perioderSøker().get(0).getAktiviteter().get(0).getUtbetalingsgrad()).isEqualTo(periodeAktivitet.getUtbetalingsgrad());
        assertThat(result.get().perioderSøker().get(0).getAktiviteter().get(0).getUttakArbeidType()).isEqualTo(periodeAktivitet.getUttakArbeidType());
        assertThat(result.get().årsakFilter().søkerErMor()).isTrue();
        assertThat(result.get().årsakFilter().kreverSammenhengendeUttak()).isFalse();
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
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, false, false, false));
        scenario.medBehandlingVedtak().medVedtakstidspunkt(vedtakstidspunkt);
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));
        return behandling;
    }

    private Behandling farBehandlingMedUttak(UttakResultatPerioderEntitet perioder, LocalDateTime vedtakstidspunkt) {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        return behandlingMedUttak(perioder, scenario, vedtakstidspunkt);
    }

    private UttakPerioderDtoTjeneste tjeneste() {
        return new UttakPerioderDtoTjeneste(uttakTjeneste, new RelatertBehandlingTjeneste(repositoryProvider),
            repositoryProvider.getYtelsesFordelingRepository(),
            inntektArbeidYtelseTjeneste, repositoryProvider.getBehandlingVedtakRepository());
    }

    @Test
    public void skalHenteUttaksPerioderMedFlereAktiviteter() {
        var perioder = new UttakResultatPerioderEntitet();
        var periode1Fom = LocalDate.now();
        var periode1Tom = LocalDate.now().plusDays(10);
        var periode2Fom = LocalDate.now().plusDays(12);
        var periode2Tom = LocalDate.now().plusDays(15);
        var periode1 = periodeBuilder(periode1Fom, periode1Tom).build();
        var periode2 = periodeBuilder(periode2Fom, periode2Tom).build();

        var nyOrgnr = "123";

        periode1.leggTilAktivitet(periodeAktivitet(periode1, orgnr));
        periode1.leggTilAktivitet(periodeAktivitet(periode1, nyOrgnr));
        periode2.leggTilAktivitet(periodeAktivitet(periode2, orgnr));
        perioder.leggTilPeriode(periode1);
        perioder.leggTilPeriode(periode2);

        var behandling = morBehandlingMedUttak(perioder);

        var tjeneste = tjeneste();

        var result = tjeneste.mapFra(behandling);

        assertThat(result).isPresent();
        assertThat(result.get().perioderSøker()).hasSize(2);
        assertThat(result.get().perioderSøker().get(0).getAktiviteter()).hasSize(2);
        assertThat(result.get().perioderSøker().get(1).getAktiviteter()).hasSize(1);
        assertThat(result.get().årsakFilter().søkerErMor()).isTrue();
        assertThat(result.get().årsakFilter().kreverSammenhengendeUttak()).isFalse();
    }

    private UttakResultatPeriodeAktivitetEntitet periodeAktivitet(UttakResultatPeriodeEntitet periode, String orgnr) {
        return periodeAktivitet(periode, orgnr, InternArbeidsforholdRef.nyRef());
    }

    private UttakResultatPeriodeAktivitetEntitet periodeAktivitet(UttakResultatPeriodeEntitet periode, String orgnr, InternArbeidsforholdRef internArbeidsforholdRef) {
        var uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.virksomhet(orgnr), internArbeidsforholdRef)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        return new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet)
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(new Utbetalingsgrad(100))
            .build();
    }

    @Test
    public void skalHenteUttaksPerioderForSøkerOgAnnenpart() {
        var perioderSøker = new UttakResultatPerioderEntitet();
        var periode1FomSøker = LocalDate.now();
        var periode1TomSøker = LocalDate.now().plusDays(10);
        var periode2FomSøker = LocalDate.now().plusDays(11);
        var periode2TomSøker = LocalDate.now().plusDays(15);
        var periode1Søker = periodeBuilder(periode1FomSøker, periode1TomSøker).build();
        var periode2Søker = periodeBuilder(periode2FomSøker, periode2TomSøker).build();
        periode1Søker.leggTilAktivitet(periodeAktivitet(periode1Søker, orgnr));
        periode2Søker.leggTilAktivitet(periodeAktivitet(periode2Søker, orgnr));
        perioderSøker.leggTilPeriode(periode1Søker);
        perioderSøker.leggTilPeriode(periode2Søker);

        var perioderAnnenpart = new UttakResultatPerioderEntitet();
        var periode1FomAnnenpart = periode2TomSøker.plusDays(1);
        var periode1TomAnnenpart = periode1FomAnnenpart.plusDays(10);
        var periode1Annenpart = periodeBuilder(periode1FomAnnenpart, periode1TomAnnenpart).build();
        periode1Annenpart.leggTilAktivitet(periodeAktivitet(periode1Annenpart, orgnr));
        perioderAnnenpart.leggTilPeriode(periode1Annenpart);

        var behandlingSøker = morBehandlingMedUttak(perioderSøker, LocalDateTime.now());
        var behandlingAnnenpart = farBehandlingMedUttak(perioderAnnenpart, LocalDateTime.now().minusDays(1));
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(behandlingSøker.getFagsak(), behandlingAnnenpart.getFagsak(), behandlingSøker);

        var tjeneste = tjeneste();

        var result = tjeneste.mapFra(behandlingSøker);

        assertThat(result).isPresent();
        assertThat(result.get().perioderSøker()).hasSize(2);
        assertThat(result.get().perioderSøker().get(0).getAktiviteter()).hasSize(1);
        assertThat(result.get().perioderSøker().get(1).getAktiviteter()).hasSize(1);

        assertThat(result.get().perioderAnnenpart()).hasSize(1);
        assertThat(result.get().perioderAnnenpart().get(0).getAktiviteter()).hasSize(1);
        assertThat(result.get().årsakFilter().søkerErMor()).isTrue();
        assertThat(result.get().årsakFilter().kreverSammenhengendeUttak()).isFalse();
    }

    @Test
    public void skalHenteUttaksPerioderForSøkerOgAnnenpartKunstigArbeidsforholdPåAnnenpart() {
        var internArbeidsforholdIdSøker = InternArbeidsforholdRef.nyRef();
        var internArbeidsforholdIdAnnenPart = InternArbeidsforholdRef.nyRef();
        var perioderSøker = new UttakResultatPerioderEntitet();
        var periode1FomSøker = LocalDate.now();
        var periode1TomSøker = LocalDate.now().plusDays(10);
        var periode1Søker = periodeBuilder(periode1FomSøker, periode1TomSøker).build();
        periode1Søker.leggTilAktivitet(periodeAktivitet(periode1Søker, orgnr, internArbeidsforholdIdSøker));
        perioderSøker.leggTilPeriode(periode1Søker);

        var perioderAnnenpart = new UttakResultatPerioderEntitet();
        var periode1FomAnnenpart = periode1TomSøker.plusDays(1);
        var periode1TomAnnenpart = periode1FomAnnenpart.plusDays(10);
        var periode1Annenpart = periodeBuilder(periode1FomAnnenpart, periode1TomAnnenpart).build();
        periode1Annenpart.leggTilAktivitet(periodeAktivitet(periode1Annenpart, KUNSTIG_ORG, internArbeidsforholdIdAnnenPart));
        perioderAnnenpart.leggTilPeriode(periode1Annenpart);

        var behandlingSøker = morBehandlingMedUttak(perioderSøker, LocalDateTime.now());
        var behandlingAnnenpart = farBehandlingMedUttak(perioderAnnenpart, LocalDateTime.now().minusDays(1));

        var arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
        var builder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        builder.leggTil(arbeidsgiver, internArbeidsforholdIdSøker, EksternArbeidsforholdRef.ref("ID1"));
        inntektArbeidYtelseTjeneste.lagreArbeidsforhold(behandlingSøker.getId(), behandlingSøker.getAktørId(), builder);
        inntektArbeidYtelseTjeneste.lagreArbeidsforhold(behandlingAnnenpart.getId(), behandlingAnnenpart.getAktørId(), lagFiktivtArbeidsforholdOverstyring(internArbeidsforholdIdAnnenPart));

        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(behandlingSøker.getFagsak(), behandlingAnnenpart.getFagsak(), behandlingSøker);

        var tjeneste = tjeneste();

        var result = tjeneste.mapFra(behandlingSøker);

        assertThat(result).isPresent();
        assertThat(result.get().perioderSøker()).hasSize(1);
        assertThat(result.get().perioderSøker().get(0).getAktiviteter()).hasSize(1);

        assertThat(result.get().perioderAnnenpart()).hasSize(1);
        assertThat(result.get().perioderAnnenpart().get(0).getAktiviteter()).hasSize(1);
        assertThat(result.get().årsakFilter().søkerErMor()).isTrue();
        assertThat(result.get().årsakFilter().kreverSammenhengendeUttak()).isFalse();
    }

    private ArbeidsforholdInformasjonBuilder lagFiktivtArbeidsforholdOverstyring(InternArbeidsforholdRef internArbeidsforholdRef) {
        var arbeidsgiver = Arbeidsgiver.virksomhet(KUNSTIG_ORG);
        var builder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        builder.leggTil(arbeidsgiver, internArbeidsforholdRef, EksternArbeidsforholdRef.ref("ID2"));
        builder.leggTil(ArbeidsforholdOverstyringBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver)
                .medHandling(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER)
                .leggTilOverstyrtPeriode(LocalDate.of(2017, 1, 1), LocalDate.of(2020, 1, 1))
                .medAngittStillingsprosent(new Stillingsprosent(BigDecimal.valueOf(100))));
        return builder;
    }

    @Test
    public void dtoSkalInneholdeSamtidigUttak() {
        var perioder = new UttakResultatPerioderEntitet();


        var periode = periodeBuilder(LocalDate.now(), LocalDate.now().plusDays(2))
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
            .build();

        var uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build();

        var periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();

        periode.leggTilAktivitet(periodeAktivitet);

        perioder.leggTilPeriode(periode);

        var behandling = morBehandlingMedUttak(perioder);

        var tjeneste = tjeneste();

        var result = tjeneste.mapFra(behandling);

        assertThat(result.get().perioderSøker().get(0).isSamtidigUttak()).isEqualTo(periode.isSamtidigUttak());
        assertThat(result.get().perioderSøker().get(0).getSamtidigUttaksprosent()).isEqualTo(periode.getSamtidigUttaksprosent());
        assertThat(result.get().årsakFilter().søkerErMor()).isTrue();
        assertThat(result.get().årsakFilter().kreverSammenhengendeUttak()).isFalse();
    }

    @Test
    public void skal_setteAleneomsorgOgAnnenForelderHarRettFalse_nårYtelsefordelingMangler() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(null);
        var behandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));

        var tjeneste = tjeneste();

        var result = tjeneste.mapFra(behandling);
        assertThat(result.get().annenForelderHarRett()).isFalse();
        assertThat(result.get().aleneomsorg()).isFalse();
        assertThat(result.get().årsakFilter().søkerErMor()).isTrue();
        assertThat(result.get().årsakFilter().kreverSammenhengendeUttak()).isFalse();
    }

    @Test
    public void skal_setteFilterFar() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(null);
        var behandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));

        var tjeneste = tjeneste();

        var result = tjeneste.mapFra(behandling);
        assertThat(result.get().annenForelderHarRett()).isFalse();
        assertThat(result.get().aleneomsorg()).isFalse();
        assertThat(result.get().årsakFilter().søkerErMor()).isFalse();
    }

    @Test
    public void skal_setteAleneomsorgOgAnnenForelderHarRettTrue_nårYtelsefordelingForeliggerOgDetStemmer() {
        var perioder = new UttakResultatPerioderEntitet();

        var perioderAnnenpart = new UttakResultatPerioderEntitet();
        var periode1FomAnnenpart = LocalDate.now().plusDays(16);
        var periode1TomAnnenpart = periode1FomAnnenpart.plusDays(10);
        var periode1Annenpart = periodeBuilder(periode1FomAnnenpart, periode1TomAnnenpart).build();
        periode1Annenpart.leggTilAktivitet(periodeAktivitet(periode1Annenpart, orgnr));
        perioderAnnenpart.leggTilPeriode(periode1Annenpart);

        var behandling = morBehandlingMedUttak(perioder);

        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false, false));
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());

        var result = tjeneste().mapFra(behandling);
        assertThat(result.get().annenForelderHarRett()).isTrue();
        assertThat(result.get().aleneomsorg()).isTrue();
        assertThat(result.get().årsakFilter().søkerErMor()).isTrue();
        assertThat(result.get().årsakFilter().kreverSammenhengendeUttak()).isFalse();
    }

    private UttakResultatPeriodeEntitet.Builder periodeBuilder(LocalDate fom, LocalDate tom) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT);
    }
}
