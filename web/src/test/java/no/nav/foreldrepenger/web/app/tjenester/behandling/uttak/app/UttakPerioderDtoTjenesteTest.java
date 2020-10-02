package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPerioderDto;

public class UttakPerioderDtoTjenesteTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private final String orgnr = UUID.randomUUID().toString();
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private AbakusInMemoryInntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Before
    public void setUp() {
        arbeidsgiverTjeneste = mock(ArbeidsgiverTjeneste.class);
        when(arbeidsgiverTjeneste.hentVirksomhet(orgnr)).thenReturn(new Virksomhet.Builder().medOrgnr(orgnr).medNavn("navn").build());
        inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        uttakTjeneste = new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository());
    }

    @Test
    public void skalHenteUttaksPerioderFraRepository() {
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        var internArbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var eksternArbeidsforholdId = EksternArbeidsforholdRef.ref("ID1");
        var arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(arbeidsgiver, internArbeidsforholdId)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        UttakPeriodeType periodeType = UttakPeriodeType.MØDREKVOTE;
        UttakResultatPeriodeSøknadEntitet periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medUttakPeriodeType(periodeType)
            .medMottattDato(LocalDate.now())
            .build();
        UttakResultatPeriodeEntitet periode = periodeBuilder(LocalDate.now(), LocalDate.now().plusWeeks(2))
            .medGraderingInnvilget(true)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
            .medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(periodeSøknad)
            .build();
        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet)
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.TEN)
            .medErSøktGradering(true)
            .medUtbetalingsgrad(new Utbetalingsgrad(1))
            .build();
        perioder.leggTilPeriode(periode);

        Behandling behandling = morBehandlingMedUttak(perioder);

        var arbeidsforholdInformasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());
        arbeidsforholdInformasjonBuilder.leggTil(arbeidsgiver, internArbeidsforholdId, eksternArbeidsforholdId);
        inntektArbeidYtelseTjeneste.lagreArbeidsforhold(behandling.getId(), behandling.getAktørId(), arbeidsforholdInformasjonBuilder);

        UttakPerioderDtoTjeneste tjeneste = tjeneste();

        Optional<UttakResultatPerioderDto> result = tjeneste.mapFra(behandling);

        assertThat(result).isPresent();
        assertThat(result.get().getPerioderSøker()).hasSize(1);
        assertThat(result.get().getPerioderSøker().get(0).getFom()).isEqualTo(periode.getFom());
        assertThat(result.get().getPerioderSøker().get(0).getTom()).isEqualTo(periode.getTom());
        assertThat(result.get().getPerioderSøker().get(0).isSamtidigUttak()).isEqualTo(periode.isSamtidigUttak());
        assertThat(result.get().getPerioderSøker().get(0).getPeriodeResultatType()).isEqualTo(periode.getResultatType());
        assertThat(result.get().getPerioderSøker().get(0).getBegrunnelse()).isEqualTo(periode.getBegrunnelse());
        assertThat(result.get().getPerioderSøker().get(0).getGradertAktivitet().getArbeidsforholdId()).isEqualTo(internArbeidsforholdId.getReferanse());
        assertThat(result.get().getPerioderSøker().get(0).getGradertAktivitet().getEksternArbeidsforholdId()).isEqualTo(eksternArbeidsforholdId.getReferanse());
        assertThat(result.get().getPerioderSøker().get(0).isSamtidigUttak()).isEqualTo(periode.isSamtidigUttak());
        assertThat(result.get().getPerioderSøker().get(0).getSamtidigUttaksprosent()).isEqualTo(periode.getSamtidigUttaksprosent());
        assertThat(result.get().getPerioderSøker().get(0).getPeriodeType()).isEqualTo(periodeType);
        assertThat(result.get().getPerioderSøker().get(0).getAktiviteter()).hasSize(1);
        assertThat(result.get().getPerioderSøker().get(0).getAktiviteter().get(0).getArbeidsforholdId()).isEqualTo(periodeAktivitet.getArbeidsforholdRef().getReferanse());
        assertThat(result.get().getPerioderSøker().get(0).getAktiviteter().get(0).getEksternArbeidsforholdId()).isEqualTo(eksternArbeidsforholdId.getReferanse());
        assertThat(result.get().getPerioderSøker().get(0).getAktiviteter().get(0).getArbeidsgiver().getIdentifikator()).isEqualTo(periodeAktivitet.getArbeidsgiver().getIdentifikator());
        assertThat(result.get().getPerioderSøker().get(0).getAktiviteter().get(0).getStønadskontoType()).isEqualTo(periodeAktivitet.getTrekkonto());
        assertThat(result.get().getPerioderSøker().get(0).getAktiviteter().get(0).getTrekkdager()).isEqualTo(periodeAktivitet.getTrekkdager().decimalValue());
        assertThat(result.get().getPerioderSøker().get(0).getAktiviteter().get(0).getProsentArbeid()).isEqualTo(periodeAktivitet.getArbeidsprosent());
        assertThat(result.get().getPerioderSøker().get(0).getAktiviteter().get(0).getUtbetalingsgrad()).isEqualTo(periodeAktivitet.getUtbetalingsgrad());
        assertThat(result.get().getPerioderSøker().get(0).getAktiviteter().get(0).getUttakArbeidType()).isEqualTo(periodeAktivitet.getUttakArbeidType());
        assertThat(result.get().getPerioderSøker().get(0).getAktiviteter().get(0).getUttakArbeidType()).isEqualTo(periodeAktivitet.getUttakArbeidType());
    }

    private Behandling morBehandlingMedUttak(UttakResultatPerioderEntitet perioder) {
        return morBehandlingMedUttak(perioder, LocalDateTime.now());
    }

    private Behandling morBehandlingMedUttak(UttakResultatPerioderEntitet perioder, LocalDateTime vedtakstidspunkt) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        return behandlingMedUttak(perioder, scenario, vedtakstidspunkt);
    }

    private Behandling behandlingMedUttak(UttakResultatPerioderEntitet perioder, AbstractTestScenario<?> scenario, LocalDateTime vedtakstidspunkt) {
        scenario.medUttak(perioder);
        scenario.medDefaultOppgittDekningsgrad();
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        scenario.medBehandlingVedtak().medVedtakstidspunkt(vedtakstidspunkt);
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));
        return behandling;
    }

    private Behandling farBehandlingMedUttak(UttakResultatPerioderEntitet perioder, LocalDateTime vedtakstidspunkt) {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        return behandlingMedUttak(perioder, scenario, vedtakstidspunkt);
    }

    private UttakPerioderDtoTjeneste tjeneste() {
        return new UttakPerioderDtoTjeneste(uttakTjeneste, new RelatertBehandlingTjeneste(repositoryProvider),
            repositoryProvider.getYtelsesFordelingRepository(), new ArbeidsgiverDtoTjeneste(arbeidsgiverTjeneste),
            inntektArbeidYtelseTjeneste, repositoryProvider.getBehandlingVedtakRepository());
    }

    @Test
    public void skalHenteUttaksPerioderMedFlereAktiviteter() {
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        LocalDate periode1Fom = LocalDate.now();
        LocalDate periode1Tom = LocalDate.now().plusDays(10);
        LocalDate periode2Fom = LocalDate.now().plusDays(12);
        LocalDate periode2Tom = LocalDate.now().plusDays(15);
        UttakResultatPeriodeEntitet periode1 = periodeBuilder(periode1Fom, periode1Tom).build();
        UttakResultatPeriodeEntitet periode2 = periodeBuilder(periode2Fom, periode2Tom).build();

        String nyOrgnr = "123";
        when(arbeidsgiverTjeneste.hentVirksomhet(nyOrgnr)).thenReturn(new Virksomhet.Builder().medOrgnr(nyOrgnr).build());

        periode1.leggTilAktivitet(periodeAktivitet(periode1, orgnr));
        periode1.leggTilAktivitet(periodeAktivitet(periode1, nyOrgnr));
        periode2.leggTilAktivitet(periodeAktivitet(periode2, orgnr));
        perioder.leggTilPeriode(periode1);
        perioder.leggTilPeriode(periode2);

        var behandling = morBehandlingMedUttak(perioder);

        UttakPerioderDtoTjeneste tjeneste = tjeneste();

        Optional<UttakResultatPerioderDto> result = tjeneste.mapFra(behandling);

        assertThat(result).isPresent();
        assertThat(result.get().getPerioderSøker()).hasSize(2);
        assertThat(result.get().getPerioderSøker().get(0).getAktiviteter()).hasSize(2);
        assertThat(result.get().getPerioderSøker().get(1).getAktiviteter()).hasSize(1);
    }

    private UttakResultatPeriodeAktivitetEntitet periodeAktivitet(UttakResultatPeriodeEntitet periode, String orgnr) {
        return periodeAktivitet(periode, orgnr, InternArbeidsforholdRef.nyRef());
    }

    private UttakResultatPeriodeAktivitetEntitet periodeAktivitet(UttakResultatPeriodeEntitet periode, String orgnr, InternArbeidsforholdRef internArbeidsforholdRef) {
        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.virksomhet(orgnr), internArbeidsforholdRef)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        return new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet)
            .medArbeidsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(new Utbetalingsgrad(100))
            .build();
    }


    @Test
    public void skalSetteRiktigNavnForVirksomhet() {
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();

        UttakResultatPeriodeEntitet periode = periodeBuilder(LocalDate.now(), LocalDate.now().plusDays(2))
            .build();

        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.virksomhet(orgnr), InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();

        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();

        periode.leggTilAktivitet(periodeAktivitet);

        perioder.leggTilPeriode(periode);

        var behandling = morBehandlingMedUttak(perioder);

        UttakPerioderDtoTjeneste tjeneste = tjeneste();

        Optional<UttakResultatPerioderDto> result = tjeneste.mapFra(behandling);

        assertThat(result.get().getPerioderSøker().get(0).getAktiviteter().get(0).getArbeidsgiver().getNavn()).isNotNull();
    }

    @Test
    public void skalHenteUttaksPerioderForSøkerOgAnnenpart() {
        UttakResultatPerioderEntitet perioderSøker = new UttakResultatPerioderEntitet();
        LocalDate periode1FomSøker = LocalDate.now();
        LocalDate periode1TomSøker = LocalDate.now().plusDays(10);
        LocalDate periode2FomSøker = LocalDate.now().plusDays(11);
        LocalDate periode2TomSøker = LocalDate.now().plusDays(15);
        UttakResultatPeriodeEntitet periode1Søker = periodeBuilder(periode1FomSøker, periode1TomSøker).build();
        UttakResultatPeriodeEntitet periode2Søker = periodeBuilder(periode2FomSøker, periode2TomSøker).build();
        periode1Søker.leggTilAktivitet(periodeAktivitet(periode1Søker, orgnr));
        periode2Søker.leggTilAktivitet(periodeAktivitet(periode2Søker, orgnr));
        perioderSøker.leggTilPeriode(periode1Søker);
        perioderSøker.leggTilPeriode(periode2Søker);

        UttakResultatPerioderEntitet perioderAnnenpart = new UttakResultatPerioderEntitet();
        LocalDate periode1FomAnnenpart = periode2TomSøker.plusDays(1);
        LocalDate periode1TomAnnenpart = periode1FomAnnenpart.plusDays(10);
        UttakResultatPeriodeEntitet periode1Annenpart = periodeBuilder(periode1FomAnnenpart, periode1TomAnnenpart).build();
        periode1Annenpart.leggTilAktivitet(periodeAktivitet(periode1Annenpart, orgnr));
        perioderAnnenpart.leggTilPeriode(periode1Annenpart);

        var behandlingSøker = morBehandlingMedUttak(perioderSøker, LocalDateTime.now());
        var behandlingAnnenpart = farBehandlingMedUttak(perioderAnnenpart, LocalDateTime.now().minusDays(1));
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(behandlingSøker.getFagsak(), behandlingAnnenpart.getFagsak(), behandlingSøker);

        UttakPerioderDtoTjeneste tjeneste = tjeneste();

        Optional<UttakResultatPerioderDto> result = tjeneste.mapFra(behandlingSøker);

        assertThat(result).isPresent();
        assertThat(result.get().getPerioderSøker()).hasSize(2);
        assertThat(result.get().getPerioderSøker().get(0).getAktiviteter()).hasSize(1);
        assertThat(result.get().getPerioderSøker().get(1).getAktiviteter()).hasSize(1);

        assertThat(result.get().getPerioderAnnenpart()).hasSize(1);
        assertThat(result.get().getPerioderAnnenpart().get(0).getAktiviteter()).hasSize(1);
    }

    @Test
    public void skalHenteUttaksPerioderForSøkerOgAnnenpartKunstigArbeidsforholdPåAnnenpart() {
        var internArbeidsforholdIdSøker = InternArbeidsforholdRef.nyRef();
        var internArbeidsforholdIdAnnenPart = InternArbeidsforholdRef.nyRef();
        UttakResultatPerioderEntitet perioderSøker = new UttakResultatPerioderEntitet();
        LocalDate periode1FomSøker = LocalDate.now();
        LocalDate periode1TomSøker = LocalDate.now().plusDays(10);
        UttakResultatPeriodeEntitet periode1Søker = periodeBuilder(periode1FomSøker, periode1TomSøker).build();
        periode1Søker.leggTilAktivitet(periodeAktivitet(periode1Søker, orgnr, internArbeidsforholdIdSøker));
        perioderSøker.leggTilPeriode(periode1Søker);

        UttakResultatPerioderEntitet perioderAnnenpart = new UttakResultatPerioderEntitet();
        LocalDate periode1FomAnnenpart = periode1TomSøker.plusDays(1);
        LocalDate periode1TomAnnenpart = periode1FomAnnenpart.plusDays(10);
        UttakResultatPeriodeEntitet periode1Annenpart = periodeBuilder(periode1FomAnnenpart, periode1TomAnnenpart).build();
        periode1Annenpart.leggTilAktivitet(periodeAktivitet(periode1Annenpart, KUNSTIG_ORG, internArbeidsforholdIdAnnenPart));
        perioderAnnenpart.leggTilPeriode(periode1Annenpart);

        var behandlingSøker = morBehandlingMedUttak(perioderSøker, LocalDateTime.now());
        var behandlingAnnenpart = farBehandlingMedUttak(perioderAnnenpart, LocalDateTime.now().minusDays(1));

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
        var builder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        builder.leggTil(arbeidsgiver, internArbeidsforholdIdSøker, EksternArbeidsforholdRef.ref("ID1"));
        inntektArbeidYtelseTjeneste.lagreArbeidsforhold(behandlingSøker.getId(), behandlingSøker.getAktørId(), builder);
        inntektArbeidYtelseTjeneste.lagreArbeidsforhold(behandlingAnnenpart.getId(), behandlingAnnenpart.getAktørId(), lagFiktivtArbeidsforholdOverstyring(internArbeidsforholdIdAnnenPart));

        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(behandlingSøker.getFagsak(), behandlingAnnenpart.getFagsak(), behandlingSøker);

        UttakPerioderDtoTjeneste tjeneste = tjeneste();

        Optional<UttakResultatPerioderDto> result = tjeneste.mapFra(behandlingSøker);

        assertThat(result).isPresent();
        assertThat(result.get().getPerioderSøker()).hasSize(1);
        assertThat(result.get().getPerioderSøker().get(0).getAktiviteter()).hasSize(1);

        assertThat(result.get().getPerioderAnnenpart()).hasSize(1);
        assertThat(result.get().getPerioderAnnenpart().get(0).getAktiviteter()).hasSize(1);
    }

    private ArbeidsforholdInformasjonBuilder lagFiktivtArbeidsforholdOverstyring(InternArbeidsforholdRef internArbeidsforholdRef) {
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(KUNSTIG_ORG);
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
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();


        UttakResultatPeriodeEntitet periode = periodeBuilder(LocalDate.now(), LocalDate.now().plusDays(2))
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
            .build();

        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build();

        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();

        periode.leggTilAktivitet(periodeAktivitet);

        perioder.leggTilPeriode(periode);

        var behandling = morBehandlingMedUttak(perioder);

        UttakPerioderDtoTjeneste tjeneste = tjeneste();

        Optional<UttakResultatPerioderDto> result = tjeneste.mapFra(behandling);

        assertThat(result.get().getPerioderSøker().get(0).isSamtidigUttak()).isEqualTo(periode.isSamtidigUttak());
        assertThat(result.get().getPerioderSøker().get(0).getSamtidigUttaksprosent()).isEqualTo(periode.getSamtidigUttaksprosent());
    }

    @Test
    public void skal_setteAleneomsorgOgAnnenForelderHarRettFalse_nårYtelsefordelingMangler() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(null);
        var behandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));

        UttakPerioderDtoTjeneste tjeneste = tjeneste();

        Optional<UttakResultatPerioderDto> result = tjeneste.mapFra(behandling);
        assertThat(result.get().isAnnenForelderHarRett()).isFalse();
        assertThat(result.get().isAleneomsorg()).isFalse();
    }

    @Test
    public void skal_setteAleneomsorgOgAnnenForelderHarRettTrue_nårYtelsefordelingForeliggerOgDetStemmer() {
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();

        UttakResultatPerioderEntitet perioderAnnenpart = new UttakResultatPerioderEntitet();
        LocalDate periode1FomAnnenpart = LocalDate.now().plusDays(16);
        LocalDate periode1TomAnnenpart = periode1FomAnnenpart.plusDays(10);
        UttakResultatPeriodeEntitet periode1Annenpart = periodeBuilder(periode1FomAnnenpart, periode1TomAnnenpart).build();
        periode1Annenpart.leggTilAktivitet(periodeAktivitet(periode1Annenpart, orgnr));
        perioderAnnenpart.leggTilPeriode(periode1Annenpart);

        Behandling behandling = morBehandlingMedUttak(perioder);
        repositoryProvider.getYtelsesFordelingRepository().lagre(behandling.getId(), new OppgittRettighetEntitet(true, true, true));

        Optional<UttakResultatPerioderDto> result = tjeneste().mapFra(behandling);
        assertThat(result.get().isAnnenForelderHarRett()).isTrue();
        assertThat(result.get().isAleneomsorg()).isTrue();
    }

    private UttakResultatPeriodeEntitet.Builder periodeBuilder(LocalDate fom, LocalDate tom) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT);
    }
}
