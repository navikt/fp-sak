package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakBeregningsandelTjenesteTestUtil;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.AdopsjonGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.AnnenPartGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.ArbeidGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.BehandlingGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.DatoerGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.InngangsvilkårGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.MedlemskapGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.OpptjeningGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.RettOgOmsorgGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.RevurderingGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.SøknadGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.YtelserGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.PersonopplysningerForUttakStub;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetType;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.ArbeidsgiverIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OppgittPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Orgnummer;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.RegelGrunnlag;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;

class FastsettePerioderRegelGrunnlagByggerTest {

    private UttakRepositoryProvider repositoryProvider;
    private final AbakusInMemoryInntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();

    private FastsettePerioderRegelGrunnlagBygger grunnlagBygger;

    @BeforeEach
    void setUp() {
        repositoryProvider = new UttakRepositoryStubProvider();
        var rettOgOmsorgGrunnlagBygger = new RettOgOmsorgGrunnlagBygger(repositoryProvider,
            new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()));
        grunnlagBygger = new FastsettePerioderRegelGrunnlagBygger(
            new AnnenPartGrunnlagBygger(repositoryProvider.getFpUttakRepository()),
            new ArbeidGrunnlagBygger(repositoryProvider), new BehandlingGrunnlagBygger(),
            new DatoerGrunnlagBygger(new PersonopplysningerForUttakStub()), new MedlemskapGrunnlagBygger(), rettOgOmsorgGrunnlagBygger,
            new RevurderingGrunnlagBygger(repositoryProvider.getYtelsesFordelingRepository(),
                repositoryProvider.getFpUttakRepository()),
            new SøknadGrunnlagBygger(repositoryProvider.getYtelsesFordelingRepository()),
            new InngangsvilkårGrunnlagBygger(repositoryProvider), new OpptjeningGrunnlagBygger(),
            new AdopsjonGrunnlagBygger(), new YtelserGrunnlagBygger());
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat);
    }

    @Test
    void oppgittPeriodeSkalFåRiktigArbeidsprosent() {
        var aktivitet = AktivitetIdentifikator.forArbeid(new Orgnummer(OrgNummer.KUNSTIG_ORG),
            InternArbeidsforholdRef.nyRef().getReferanse());

        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var virksomhet = lagVirksomhetArbeidsgiver(aktivitet.getArbeidsgiverIdentifikator());

        var arbeidsprosentFraSøknad = BigDecimal.valueOf(60);
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(arbeidsprosentFraSøknad)
            .medArbeidsgiver(virksomhet)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .build();

        var behandling = setup(oppgittPeriode, virksomhet, BigDecimal.valueOf(100), aktivitet.getArbeidsforholdId(),
            fom.minusYears(1));

        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet,
            InternArbeidsforholdRef.ref(aktivitet.getArbeidsforholdId()));

        var input = lagInput(behandling);
        var grunnlag = grunnlagBygger.byggGrunnlag(input, lagStønadskontoer());

        assertThat(grunnlag.getSøknad().getOppgittePerioder()).hasSize(1);
        var oppgittPeriodeIGrunnlag = grunnlag.getSøknad().getOppgittePerioder().getFirst();
        assertThat(oppgittPeriodeIGrunnlag.getArbeidsprosent()).isEqualTo(arbeidsprosentFraSøknad);
    }

    private UttakInput lagInput(Behandling behandling) {
        var ref = BehandlingReferanse.fra(behandling);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.behandlingId());
        var bekreftetFamilieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now().minusWeeks(2),
            List.of(new Barn()), 1);
        var fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(
            new FamilieHendelser().medBekreftetHendelse(bekreftetFamilieHendelse));
        return new UttakInput(ref, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build(), iayGrunnlag, fpGrunnlag).medBeregningsgrunnlagStatuser(
            beregningsandelTjeneste.hentStatuser());
    }

    @Test
    void skalLeggeTilFlereGraderteAktiviteterIUttakPeriodeVedFlereGraderteAktiviterISammeVirksomhet() {
        var orgnr = new Orgnummer(OrgNummer.KUNSTIG_ORG);
        var aktivitet1 = AktivitetIdentifikator.forArbeid(orgnr, InternArbeidsforholdRef.nyRef().getReferanse());
        var aktivitet2 = AktivitetIdentifikator.forArbeid(orgnr, InternArbeidsforholdRef.nyRef().getReferanse());

        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var virksomhet = lagVirksomhetArbeidsgiver(orgnr);

        var arbeidsprosent = BigDecimal.valueOf(60);
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(arbeidsprosent)
            .medArbeidsgiver(virksomhet)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .build();

        var oppgittPeriode1 = OppgittPeriodeBuilder.ny()
            .medPeriode(tom.plusDays(1), tom.plusDays(4))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        var behandling = setup(List.of(oppgittPeriode, oppgittPeriode1), virksomhet, BigDecimal.valueOf(100),
            List.of(aktivitet1.getArbeidsforholdId(), aktivitet2.getArbeidsforholdId()), fom.minusYears(1));


        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet,
            InternArbeidsforholdRef.ref(aktivitet1.getArbeidsforholdId()));
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet,
            InternArbeidsforholdRef.ref(aktivitet2.getArbeidsforholdId()));
        var input = lagInput(behandling);
        var grunnlag = grunnlagBygger.byggGrunnlag(input, lagStønadskontoer());

        assertThat(grunnlag.getSøknad().getOppgittePerioder()).hasSize(2);
        var gradertOppgittPeriode = finnGradertUttakPeriode(arbeidsprosent, grunnlag);
        assertThat(gradertOppgittPeriode).isPresent();
        assertThat(gradertOppgittPeriode.orElseThrow().getGradertAktiviteter()).hasSize(2);
        assertThat(gradertAktivitetMedIdentifikator(aktivitet1, gradertOppgittPeriode.orElseThrow())).isPresent();
        assertThat(gradertAktivitetMedIdentifikator(aktivitet2, gradertOppgittPeriode.orElseThrow())).isPresent();
    }

    private Optional<AktivitetIdentifikator> gradertAktivitetMedIdentifikator(AktivitetIdentifikator aktivitetIdentifikator,
                                                                              OppgittPeriode gradertOppgittPeriode) {
        return gradertOppgittPeriode.getGradertAktiviteter()
            .stream()
            .filter(a -> a.getArbeidsgiverIdentifikator().equals(aktivitetIdentifikator.getArbeidsgiverIdentifikator()))
            .filter(a -> a.getArbeidsforholdId().equals(aktivitetIdentifikator.getArbeidsforholdId()))
            .findFirst();
    }

    private Optional<OppgittPeriode> finnGradertUttakPeriode(BigDecimal arbeidsprosent, RegelGrunnlag grunnlag) {
        for (var oppgittPeriode : grunnlag.getSøknad().getOppgittePerioder()) {
            if (arbeidsprosent.equals(oppgittPeriode.getArbeidsprosent())) {
                return Optional.of(oppgittPeriode);
            }
        }
        return Optional.empty();
    }

    @Test
    void skalLeggeTilGradertFrilansUtenOrgnrISøknadSomGradertPeriode() {
        var orgnr = new Orgnummer(OrgNummer.KUNSTIG_ORG);

        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var virksomhet = lagVirksomhetArbeidsgiver(orgnr);

        var arbeidsprosent = BigDecimal.valueOf(60);
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(arbeidsprosent)
            .medGraderingAktivitetType(GraderingAktivitetType.FRILANS)
            .build();

        var behandling = setupFrilans(oppgittPeriode, virksomhet);

        beregningsandelTjeneste.leggTilFrilans();
        var input = lagInput(behandling);

        var grunnlag = grunnlagBygger.byggGrunnlag(input, lagStønadskontoer());

        assertThat(grunnlag.getSøknad().getOppgittePerioder()).hasSize(1);
        assertThat(grunnlag.getSøknad().getOppgittePerioder().getFirst().getArbeidsprosent()).isEqualTo(arbeidsprosent);
        assertThat(grunnlag.getSøknad()
            .getOppgittePerioder()
            .getFirst()
            .getGradertAktiviteter()
            .stream()
            .findFirst()
            .orElseThrow()
            .getAktivitetType()).isEqualTo(AktivitetType.FRILANS);
    }

    @Test
    void gradertArbeidsforholdArbeidstakerMedSamtUgradertFrilansSkalBareGiEnGradertAktivitet() {
        var orgnr = new Orgnummer(OrgNummer.KUNSTIG_ORG);

        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var arbeidsgiver = lagVirksomhetArbeidsgiver(orgnr);

        var arbeidsprosent = BigDecimal.valueOf(60);
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(arbeidsprosent)
            .medArbeidsgiver(arbeidsgiver)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .build();

        var behandling = setupScenario(Collections.singletonList(oppgittPeriode));

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var builder = lagYrkesAktiviter(behandling, arbeidsgiver,
            Collections.singletonList(arbeidsforholdId.getReferanse()), BigDecimal.valueOf(100),
            LocalDate.now().minusWeeks(1));

        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        var yrkesaktivitetBuilder = lagYrkesAktivitetForArbeidsforhold(arbeidsgiver, BigDecimal.valueOf(100),
            LocalDate.now().minusWeeks(1), LocalDate.now().plusMonths(1), arbeidsforholdId,
            ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER);
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);


        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver, arbeidsforholdId);
        beregningsandelTjeneste.leggTilFrilans();

        var input = lagInput(behandling);
        var grunnlag = grunnlagBygger.byggGrunnlag(input, lagStønadskontoer());

        assertThat(grunnlag.getSøknad().getOppgittePerioder()).hasSize(1);
        assertThat(grunnlag.getSøknad().getOppgittePerioder().getFirst().getArbeidsprosent()).isEqualTo(arbeidsprosent);
        assertThat(grunnlag.getSøknad().getOppgittePerioder().getFirst().getGradertAktiviteter()).hasSize(1);
        var aktivitetIdentifikator1 = grunnlag.getSøknad()
            .getOppgittePerioder()
            .getFirst()
            .getGradertAktiviteter()
            .stream()
            .findFirst()
            .orElseThrow();
        assertThat(aktivitetIdentifikator1.getAktivitetType()).isEqualTo(AktivitetType.ARBEID);
        assertThat(aktivitetIdentifikator1.getArbeidsgiverIdentifikator()).isEqualTo(orgnr);
    }

    @Test
    void mapperAnnenPartsUttaksperioder() {
        // Arrange - mors behandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var fødselsdato = LocalDate.of(2018, 5, 14);
        var morsBehandling = lagre(scenario);
        var perioder = new UttakResultatPerioderEntitet();


        // Uttak periode 1
        var uttakMødrekvote = new UttakResultatPeriodeEntitet.Builder(fødselsdato,
            fødselsdato.plusWeeks(6).minusDays(1)).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.UKJENT).build();

        var morsArbeidsgiver = lagVirksomhetArbeidsgiver(new Orgnummer("3333"));
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(morsArbeidsgiver, InternArbeidsforholdRef.nyRef())
            .build();

        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(30))
            .medTrekkonto(UttakPeriodeType.MØDREKVOTE)
            .medUtbetalingsgrad(Utbetalingsgrad.TEN)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();

        perioder.leggTilPeriode(uttakMødrekvote);

        // Uttak periode 2
        var uttakFellesperiode = new UttakResultatPeriodeEntitet.Builder(fødselsdato.plusWeeks(6),
            fødselsdato.plusWeeks(10).minusDays(1)).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.UKJENT).build();

        UttakResultatPeriodeAktivitetEntitet.builder(uttakFellesperiode, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(20))
            .medTrekkonto(UttakPeriodeType.FELLESPERIODE)
            .medUtbetalingsgrad(Utbetalingsgrad.TEN)
            .medArbeidsprosent(BigDecimal.valueOf(10))
            .build();

        perioder.leggTilPeriode(uttakFellesperiode);

        //Utsettelse
        var utsettelse = new UttakResultatPeriodeEntitet.Builder(fødselsdato.plusWeeks(10),
            fødselsdato.plusWeeks(11).minusDays(1)).medResultatType(PeriodeResultatType.INNVILGET,
            PeriodeResultatÅrsak.UKJENT).build();

        UttakResultatPeriodeAktivitetEntitet.builder(utsettelse, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(20))
            .medTrekkonto(UttakPeriodeType.UDEFINERT)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();

        perioder.leggTilPeriode(utsettelse);

        var kontoer = lagStønadskontoer();

        repositoryProvider.getFpUttakRepository()
            .lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), kontoer, perioder);

        // Arrange - fars behandling
        var aktivitet = AktivitetIdentifikator.forArbeid(new Orgnummer("1111"),
            InternArbeidsforholdRef.nyRef().getReferanse());
        var virksomhet = lagVirksomhetArbeidsgiver(aktivitet.getArbeidsgiverIdentifikator());
        var uttakFPFar = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(20).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.ZERO)
            .medArbeidsgiver(virksomhet)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .build();

        var avklarteUttakDatoerEntitet = new AvklarteUttakDatoerEntitet.Builder().medJustertEndringsdato(fødselsdato)
            .build();
        var scenarioFarSøkerForeldrepenger = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(uttakFPFar), true))
            .medAvklarteUttakDatoer(avklarteUttakDatoerEntitet)
            .medDefaultInntektArbeidYtelse()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        var farsBehandling = lagre(scenarioFarSøkerForeldrepenger);

        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(morsBehandling.getFagsak());
        repositoryProvider.getFagsakRelasjonRepository()
            .kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak());

        lagreUttaksperiodegrense(repositoryProvider.getUttaksperiodegrenseRepository(), farsBehandling.getId());
        lagreYrkesAktiviter(farsBehandling, virksomhet, Collections.singletonList(aktivitet.getArbeidsforholdId()),
            BigDecimal.valueOf(100), fødselsdato.minusYears(1));

        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet,
            InternArbeidsforholdRef.ref(aktivitet.getArbeidsforholdId()));
        // Act
        var ref = BehandlingReferanse.fra(farsBehandling);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.behandlingId());
        var bekreftetFamilieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now().minusWeeks(2),
            List.of(new Barn()), 1);
        var fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(
            new FamilieHendelser().medBekreftetHendelse(bekreftetFamilieHendelse))
            .medAnnenpart(new Annenpart(morsBehandling.getId(), LocalDateTime.now()));
        var input = new UttakInput(ref, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build(), iayGrunnlag, fpGrunnlag).medBeregningsgrunnlagStatuser(
            beregningsandelTjeneste.hentStatuser());
        var grunnlag = grunnlagBygger.byggGrunnlag(input, kontoer);

        // Assert
        var forventetOrgnr = new Orgnummer(arbeidsforhold1.getArbeidsgiver().orElseThrow().getIdentifikator());
        var forventetAktivitetIdentifikator = AktivitetIdentifikator.forArbeid(forventetOrgnr,
            arbeidsforhold1.getArbeidsforholdRef().getReferanse());

        var uttakPerioderAnnenPart = grunnlag.getAnnenPart().getUttaksperioder();
        assertThat(uttakPerioderAnnenPart).hasSize(3);

        var annenPartGrunnlag = uttakPerioderAnnenPart.getFirst();
        assertThat(annenPartGrunnlag.getFom()).isEqualTo(uttakMødrekvote.getFom());
        assertThat(annenPartGrunnlag.getTom()).isEqualTo(uttakMødrekvote.getTom());
        assertThat(annenPartGrunnlag.getAktiviteter()).hasSize(1);

        var aktivitetMødrekvote = annenPartGrunnlag.getAktiviteter().stream().findFirst().orElseThrow();
        assertThat(aktivitetMødrekvote.getAktivitetIdentifikator()).isEqualTo(forventetAktivitetIdentifikator);
        assertThat(aktivitetMødrekvote.getStønadskontotype()).isEqualTo(Stønadskontotype.MØDREKVOTE);
        assertThat(new Trekkdager(aktivitetMødrekvote.getTrekkdager().decimalValue())).isEqualTo(
            uttakMødrekvote.getAktiviteter().getFirst().getTrekkdager());

        var mappedFellesperiode = uttakPerioderAnnenPart.get(1);
        assertThat(mappedFellesperiode.getFom()).isEqualTo(uttakFellesperiode.getFom());
        assertThat(mappedFellesperiode.getTom()).isEqualTo(uttakFellesperiode.getTom());
        assertThat(mappedFellesperiode.getAktiviteter()).hasSize(1);

        var aktivitetFellesperiode = mappedFellesperiode.getAktiviteter().stream().findFirst().orElseThrow();
        assertThat(aktivitetFellesperiode.getAktivitetIdentifikator()).isEqualTo(forventetAktivitetIdentifikator);
        assertThat(aktivitetFellesperiode.getStønadskontotype()).isEqualTo(Stønadskontotype.FELLESPERIODE);
        assertThat(new Trekkdager(aktivitetFellesperiode.getTrekkdager().decimalValue())).isEqualTo(
            uttakFellesperiode.getAktiviteter().getFirst().getTrekkdager());
    }

    @Test
    void testeSamtidigUttak() {
        var orgnr1 = new Orgnummer(OrgNummer.KUNSTIG_ORG);
        var aktivitet = AktivitetIdentifikator.forArbeid(orgnr1, InternArbeidsforholdRef.nyRef().getReferanse());

        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var virksomhet = lagVirksomhetArbeidsgiver(orgnr1);

        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsgiver(virksomhet)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
            .build();

        var behandling = setupScenario(Collections.singletonList(oppgittPeriode));
        lagreYrkesAktiviter(behandling, virksomhet, Collections.singletonList(aktivitet.getArbeidsforholdId()),
            BigDecimal.valueOf(100), fom.minusYears(1));


        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet,
            InternArbeidsforholdRef.ref(aktivitet.getArbeidsforholdId()));

        var input = lagInput(behandling);
        var grunnlag = grunnlagBygger.byggGrunnlag(input, lagStønadskontoer());

        assertThat(grunnlag.getArbeid().getAktiviteter()).hasSize(1);

        var oppittePerioder = grunnlag.getSøknad().getOppgittePerioder();

        assertThat(oppittePerioder).isNotEmpty();
        assertThat(oppittePerioder.getFirst().getSamtidigUttaksprosent().decimalValue()).isEqualTo(
            SamtidigUttaksprosent.TEN.decimalValue());
    }

    private Behandling setup(OppgittPeriodeEntitet oppgittPeriode,
                             Arbeidsgiver arbeidsgiver,
                             BigDecimal stillingsprosent,
                             String arbeidsforholdId,
                             LocalDate startdatoArbeidsgiver) {
        return setup(Collections.singletonList(oppgittPeriode), arbeidsgiver, stillingsprosent,
            List.of(arbeidsforholdId), startdatoArbeidsgiver);
    }

    private Behandling setup(List<OppgittPeriodeEntitet> oppgittPeriode,
                             Arbeidsgiver arbeidsgiver,
                             BigDecimal stillingsprosent,
                             List<String> arbeidsforholdIdList,
                             LocalDate startdatoArbeidsgiver) {
        var behandling = setupScenario(oppgittPeriode);
        lagreYrkesAktiviter(behandling, arbeidsgiver, arbeidsforholdIdList, stillingsprosent, startdatoArbeidsgiver);
        return behandling;
    }

    private Behandling setupFrilans(OppgittPeriodeEntitet oppgittPeriode, Arbeidsgiver arbeidsgiver) {
        var behandling = setupScenario(Collections.singletonList(oppgittPeriode));
        lagreYrkesAktiviterFrilans(behandling, arbeidsgiver);
        return behandling;
    }

    private void lagreUttaksperiodegrense(UttaksperiodegrenseRepository repository, Long behandlingId) {
        var grense = new Uttaksperiodegrense(LocalDate.now().minusWeeks(2));
        repository.lagre(behandlingId, grense);
    }

    private Arbeidsgiver lagVirksomhetArbeidsgiver(ArbeidsgiverIdentifikator arbeidsgiverIdentifikator) {
        return Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator.value());
    }

    private Stønadskontoberegning lagStønadskontoer() {
        var mødrekvote = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.MØDREKVOTE)
            .medMaxDager(30)
            .build();
        var fellesperiode = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .medMaxDager(15)
            .build();
        var fedrekvote = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FEDREKVOTE)
            .medMaxDager(50)
            .build();
        var foreldrepengerFørFødsel = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
            .medMaxDager(130)
            .build();

        return Stønadskontoberegning.builder()
            .medStønadskonto(mødrekvote)
            .medStønadskonto(fedrekvote)
            .medStønadskonto(fellesperiode)
            .medStønadskonto(foreldrepengerFørFødsel)
            .medRegelEvaluering(" ")
            .medRegelInput(" ")
            .build();
    }

    private void lagreYrkesAktiviter(Behandling behandling,
                                     Arbeidsgiver virksomhet,
                                     List<String> arbeidsforholdIdList,
                                     BigDecimal stillingsprosent,
                                     LocalDate fom) {
        var builder = lagYrkesAktiviter(behandling, virksomhet, arbeidsforholdIdList, stillingsprosent, fom);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private InntektArbeidYtelseAggregatBuilder lagYrkesAktiviter(Behandling behandling,
                                                                 Arbeidsgiver virksomhet,
                                                                 List<String> arbeidsforholdIdList,
                                                                 BigDecimal stillingsprosent,
                                                                 LocalDate fom) {
        var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());

        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());

        builder.leggTilAktørArbeid(aktørArbeidBuilder);

        for (var arbeidsforholdId : arbeidsforholdIdList) {
            var yrkesaktivitetBuilder = lagYrkesAktivitetForArbeidsforhold(virksomhet, stillingsprosent, fom,
                LocalDate.MAX, InternArbeidsforholdRef.ref(arbeidsforholdId), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

            aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
            iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        }
        return builder;
    }

    private void lagreYrkesAktiviterFrilans(Behandling behandling, Arbeidsgiver virksomhet) {
        var builder = lagYrkesAktivitFrilans(behandling, virksomhet);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private InntektArbeidYtelseAggregatBuilder lagYrkesAktivitFrilans(Behandling behandling, Arbeidsgiver virksomhet) {
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);

        var fraOgMed = LocalDate.now().minusWeeks(1);
        var tilOgMed = LocalDate.now().plusMonths(1);

        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        builder.leggTilAktørArbeid(aktørArbeidBuilder);

        var yrkesaktivitetBuilder = lagYrkesAktivitetForArbeidsforhold(virksomhet, BigDecimal.valueOf(100), fraOgMed,
            tilOgMed, null, ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER);

        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        var yrkesaktivitetBuilder2 = lagYrkesAktivitetForFrilansOverordnet(fraOgMed, tilOgMed,
            ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER);

        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder2);
        return builder;
    }

    private YrkesaktivitetBuilder lagYrkesAktivitetForFrilansOverordnet(LocalDate fraOgMed,
                                                                        LocalDate tilOgMed,
                                                                        ArbeidType arbeidType) {
        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder.medPeriode(
            DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed));
        yrkesaktivitetBuilder.medArbeidType(arbeidType).leggTilAktivitetsAvtale(aktivitetsAvtale);

        return yrkesaktivitetBuilder;
    }

    private YrkesaktivitetBuilder lagYrkesAktivitetForArbeidsforhold(Arbeidsgiver virksomhet,
                                                                     BigDecimal stillingsprosent,
                                                                     LocalDate fraOgMed,
                                                                     LocalDate tilOgMed,
                                                                     InternArbeidsforholdRef arbeidsforholdId,
                                                                     ArbeidType arbeidType) {
        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());

        var aktivitetsAvtale = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed))
            .medProsentsats(stillingsprosent)
            .medSisteLønnsendringsdato(fraOgMed);
        var ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed));
        yrkesaktivitetBuilder.medArbeidType(arbeidType)
            .medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(arbeidsforholdId)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode);

        return yrkesaktivitetBuilder;
    }

    private Behandling setupScenario(List<OppgittPeriodeEntitet> oppgittPerioder) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(new OppgittFordelingEntitet(oppgittPerioder, true));
        scenario.medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        var førsteUttaksdag = oppgittPerioder.stream()
            .min(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .get()
            .getFom();
        scenario.medAvklarteUttakDatoer(
            new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(førsteUttaksdag).build());

        var behandling = lagre(scenario);
        lagreUttaksperiodegrense(repositoryProvider.getUttaksperiodegrenseRepository(), behandling.getId());
        return behandling;
    }
}
