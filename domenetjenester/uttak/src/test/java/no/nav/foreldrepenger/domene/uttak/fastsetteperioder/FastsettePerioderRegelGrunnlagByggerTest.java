package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
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
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.UttakBeregningsandelTjenesteTestUtil;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetType;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AnnenpartUttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OppgittPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.RegelGrunnlag;
import no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Stønadskontotype;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.Repository;

@RunWith(CdiRunner.class)
public class FastsettePerioderRegelGrunnlagByggerTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private UttakRepositoryProvider repositoryProvider = new UttakRepositoryProvider(repositoryRule.getEntityManager());
    private final AbakusInMemoryInntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private final UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();

    @Inject
    private FastsettePerioderRegelGrunnlagBygger grunnlagBygger;

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider, iayTjeneste::lagreIayAggregat, iayTjeneste::lagreOppgittOpptjening);
    }

    @Test
    public void oppgittPeriodeSkalFåRiktigArbeidsprosent() {
        AktivitetIdentifikator aktivitet = AktivitetIdentifikator.forArbeid(OrgNummer.KUNSTIG_ORG, InternArbeidsforholdRef.nyRef().getReferanse());

        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusDays(10);
        Arbeidsgiver virksomhet = lagVirksomhetArbeidsgiver(aktivitet.getArbeidsgiverIdentifikator());

        BigDecimal arbeidsprosentFraSøknad = BigDecimal.valueOf(60);
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(arbeidsprosentFraSøknad)
            .medArbeidsgiver(virksomhet)
            .medErArbeidstaker(true)
            .build();

        Behandling behandling = setup(oppgittPeriode, virksomhet, BigDecimal.valueOf(100), aktivitet.getArbeidsforholdId(),
            fom.minusYears(1));

        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, InternArbeidsforholdRef.ref(aktivitet.getArbeidsforholdId()));

        var input = lagInput(behandling);
        RegelGrunnlag grunnlag = grunnlagBygger.byggGrunnlag(input);

        assertThat(grunnlag.getSøknad().getOppgittePerioder()).hasSize(1);
        var oppgittPeriodeIGrunnlag = grunnlag.getSøknad().getOppgittePerioder().get(0);
        assertThat(oppgittPeriodeIGrunnlag.getArbeidsprosent()).isEqualTo(arbeidsprosentFraSøknad);
    }

    private UttakInput lagInput(Behandling behandling) {
        var ref = lagRef(behandling);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
        var bekreftetFamilieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now().minusWeeks(2), List.of(new Barn()), 1);
        ForeldrepengerGrunnlag fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(bekreftetFamilieHendelse));
        return new UttakInput(ref, iayGrunnlag, fpGrunnlag).medBeregningsgrunnlagStatuser(beregningsandelTjeneste.hentStatuser());
    }

    @Test
    public void skalLeggeTilFlereGraderteAktiviteterIUttakPeriodeVedFlereGraderteAktiviterISammeVirksomhet() {
        String orgnr = OrgNummer.KUNSTIG_ORG;
        AktivitetIdentifikator aktivitet1 = AktivitetIdentifikator.forArbeid(orgnr, InternArbeidsforholdRef.nyRef().getReferanse());
        AktivitetIdentifikator aktivitet2 = AktivitetIdentifikator.forArbeid(orgnr, InternArbeidsforholdRef.nyRef().getReferanse());

        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusDays(10);
        Arbeidsgiver virksomhet = lagVirksomhetArbeidsgiver(orgnr);

        BigDecimal arbeidsprosent = BigDecimal.valueOf(60);
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(arbeidsprosent)
            .medArbeidsgiver(virksomhet)
            .medErArbeidstaker(true)
            .build();

        OppgittPeriodeEntitet oppgittPeriode1 = OppgittPeriodeBuilder.ny()
            .medPeriode(tom.plusDays(1), tom.plusDays(4))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        Behandling behandling = setup(List.of(oppgittPeriode, oppgittPeriode1), virksomhet, BigDecimal.valueOf(100),
            List.of(aktivitet1.getArbeidsforholdId(), aktivitet2.getArbeidsforholdId()), fom.minusYears(1));


        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, InternArbeidsforholdRef.ref(aktivitet1.getArbeidsforholdId()));
        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, InternArbeidsforholdRef.ref(aktivitet2.getArbeidsforholdId()));
        var input = lagInput(behandling);
        RegelGrunnlag grunnlag = grunnlagBygger.byggGrunnlag(input);

        assertThat(grunnlag.getSøknad().getOppgittePerioder()).hasSize(2);
        var gradertOppgittPeriode = finnGradertUttakPeriode(arbeidsprosent, grunnlag);
        assertThat(gradertOppgittPeriode).isPresent();
        assertThat(gradertOppgittPeriode.orElseThrow().getGradertAktiviteter()).hasSize(2);
        assertThat(gradertAktivitetMedIdentifikator(aktivitet1, gradertOppgittPeriode.orElseThrow())).isPresent();
        assertThat(gradertAktivitetMedIdentifikator(aktivitet2, gradertOppgittPeriode.orElseThrow())).isPresent();
    }

    private Optional<AktivitetIdentifikator> gradertAktivitetMedIdentifikator(AktivitetIdentifikator aktivitetIdentifikator, OppgittPeriode gradertOppgittPeriode) {
        return gradertOppgittPeriode.getGradertAktiviteter().stream()
            .filter(a -> a.getArbeidsgiverIdentifikator().equals(aktivitetIdentifikator.getArbeidsgiverIdentifikator()))
            .filter(a -> a.getArbeidsforholdId().equals(aktivitetIdentifikator.getArbeidsforholdId()))
            .findFirst();
    }

    private Optional<OppgittPeriode> finnGradertUttakPeriode(BigDecimal arbeidsprosent, RegelGrunnlag grunnlag) {
        for (OppgittPeriode oppgittPeriode : grunnlag.getSøknad().getOppgittePerioder()) {
            if (arbeidsprosent.equals(oppgittPeriode.getArbeidsprosent())) {
                return Optional.of(oppgittPeriode);
            }
        }
        return Optional.empty();
    }

    @Test
    public void skalLeggeTilGradertFrilansUtenOrgnrISøknadSomGradertPeriode() {
        String orgnr = OrgNummer.KUNSTIG_ORG;

        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusDays(10);
        Arbeidsgiver virksomhet = lagVirksomhetArbeidsgiver(orgnr);

        BigDecimal arbeidsprosent = BigDecimal.valueOf(60);
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(arbeidsprosent)
            .medErFrilanser(true)
            .build();

        Behandling behandling = setupFrilans(oppgittPeriode, virksomhet);

        beregningsandelTjeneste.leggTilFrilans();
        var input = lagInput(behandling);

        RegelGrunnlag grunnlag = grunnlagBygger.byggGrunnlag(input);

        assertThat(grunnlag.getSøknad().getOppgittePerioder()).hasSize(1);
        assertThat(grunnlag.getSøknad().getOppgittePerioder().get(0).getArbeidsprosent()).isEqualTo(arbeidsprosent);
        assertThat(grunnlag.getSøknad().getOppgittePerioder().get(0).getGradertAktiviteter().stream().findFirst().orElseThrow().getAktivitetType()).isEqualTo(AktivitetType.FRILANS);
    }

    @Test
    public void gradertArbeidsforholdArbeidstakerMedSamtUgradertFrilansSkalBareGiEnGradertAktivitet() {
        String orgnr = OrgNummer.KUNSTIG_ORG;

        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusDays(10);
        Arbeidsgiver arbeidsgiver = lagVirksomhetArbeidsgiver(orgnr);

        BigDecimal arbeidsprosent = BigDecimal.valueOf(60);
        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(arbeidsprosent)
            .medArbeidsgiver(arbeidsgiver)
            .medErArbeidstaker(true)
            .build();

        Behandling behandling = setupScenario(Collections.singletonList(oppgittPeriode));

        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        InntektArbeidYtelseAggregatBuilder builder = lagYrkesAktiviter(behandling, arbeidsgiver,
            Collections.singletonList(arbeidsforholdId.getReferanse()), BigDecimal.valueOf(100), LocalDate.now().minusWeeks(1));

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        YrkesaktivitetBuilder yrkesaktivitetBuilder = lagYrkesAktivitetForArbeidsforhold(arbeidsgiver,
            BigDecimal.valueOf(100), LocalDate.now().minusWeeks(1), LocalDate.now().plusMonths(1),
            arbeidsforholdId,
            ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER);
        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);


        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver, arbeidsforholdId);
        beregningsandelTjeneste.leggTilFrilans();

        var input = lagInput(behandling);
        RegelGrunnlag grunnlag = grunnlagBygger.byggGrunnlag(input);

        assertThat(grunnlag.getSøknad().getOppgittePerioder()).hasSize(1);
        assertThat(grunnlag.getSøknad().getOppgittePerioder().get(0).getArbeidsprosent()).isEqualTo(arbeidsprosent);
        assertThat(grunnlag.getSøknad().getOppgittePerioder().get(0).getGradertAktiviteter()).hasSize(1);
        var aktivitetIdentifikator1 = grunnlag.getSøknad().getOppgittePerioder().get(0).getGradertAktiviteter().stream().findFirst().orElseThrow();
        assertThat(aktivitetIdentifikator1.getAktivitetType()).isEqualTo(AktivitetType.ARBEID);
        assertThat(aktivitetIdentifikator1.getArbeidsgiverIdentifikator()).isEqualTo(orgnr);
    }

    @Test
    public void mapperAnnenPartsUttaksperioder() {
        Repository repository = repositoryRule.getRepository();

        // Arrange - mors behandling
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        LocalDate fødselsdato = LocalDate.of(2018, 5, 14);
        Behandling morsBehandling = lagre(scenario);
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();


        // Uttak periode 1
        UttakResultatPeriodeEntitet uttakMødrekvote = new UttakResultatPeriodeEntitet.Builder(fødselsdato, fødselsdato.plusWeeks(6).minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();

        Arbeidsgiver morsArbeidsgiver = lagVirksomhetArbeidsgiver("3333");
        UttakAktivitetEntitet arbeidsforhold1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID).medArbeidsforhold(morsArbeidsgiver, InternArbeidsforholdRef.nyRef())
            .build();

        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(30))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medUtbetalingsgrad(BigDecimal.TEN)
            .medArbeidsprosent(BigDecimal.ZERO).build();

        perioder.leggTilPeriode(uttakMødrekvote);

        // Uttak periode 2
        UttakResultatPeriodeEntitet uttakFellesperiode = new UttakResultatPeriodeEntitet.Builder(fødselsdato.plusWeeks(6), fødselsdato.plusWeeks(10).minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();

        UttakResultatPeriodeAktivitetEntitet.builder(uttakFellesperiode, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(20))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medUtbetalingsgrad(BigDecimal.TEN)
            .medArbeidsprosent(BigDecimal.valueOf(10)).build();

        perioder.leggTilPeriode(uttakFellesperiode);

        //Utsettelse
        UttakResultatPeriodeEntitet utsettelse = new UttakResultatPeriodeEntitet.Builder(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(11).minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();

        UttakResultatPeriodeAktivitetEntitet.builder(utsettelse, arbeidsforhold1)
            .medTrekkdager(new Trekkdager(20))
            .medTrekkonto(StønadskontoType.UDEFINERT)
            .medUtbetalingsgrad(BigDecimal.ZERO)
            .medArbeidsprosent(BigDecimal.ZERO).build();

        perioder.leggTilPeriode(utsettelse);

        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), perioder);

        morsBehandling.avsluttBehandling();
        repository.lagre(morsBehandling);

        lagreStønadskontoer(morsBehandling, repositoryProvider.getFagsakRelasjonRepository());

        // Arrange - fars behandling
        AktivitetIdentifikator aktivitet = AktivitetIdentifikator.forArbeid("1111", InternArbeidsforholdRef.nyRef().getReferanse());
        Arbeidsgiver virksomhet = lagVirksomhetArbeidsgiver(aktivitet.getArbeidsgiverIdentifikator());
        OppgittPeriodeEntitet uttakFPFar = OppgittPeriodeBuilder.ny()
            .medPeriode(fødselsdato.plusWeeks(10), fødselsdato.plusWeeks(20).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.ZERO)
            .medArbeidsgiver(virksomhet)
            .medErArbeidstaker(true)
            .build();

        ScenarioFarSøkerForeldrepenger scenarioFarSøkerForeldrepenger = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(uttakFPFar), true));
        scenarioFarSøkerForeldrepenger.medDefaultInntektArbeidYtelse();
        scenarioFarSøkerForeldrepenger.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        Behandling farsBehandling = lagre(scenarioFarSøkerForeldrepenger);

        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak(), morsBehandling);

        lagreUttaksperiodegrense(farsBehandling, repositoryProvider.getUttaksperiodegrenseRepository());
        repositoryProvider.getYtelsesFordelingRepository().lagre(farsBehandling.getId(), new AvklarteUttakDatoerEntitet.Builder().medJustertEndringsdato(fødselsdato).build());
        lagreYrkesAktiviter(farsBehandling, virksomhet, Collections.singletonList(aktivitet.getArbeidsforholdId()),
            BigDecimal.valueOf(100), fødselsdato.minusYears(1));
        repository.flushAndClear();


        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, InternArbeidsforholdRef.ref(aktivitet.getArbeidsforholdId()));
        // Act
        var ref = lagRef(farsBehandling);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
        var bekreftetFamilieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now().minusWeeks(2), List.of(new Barn()), 1);
        ForeldrepengerGrunnlag fpGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(bekreftetFamilieHendelse))
            .medAnnenpart(new Annenpart(false, morsBehandling.getId()));
        var input = new UttakInput(ref, iayGrunnlag, fpGrunnlag)
            .medBeregningsgrunnlagStatuser(beregningsandelTjeneste.hentStatuser());
        RegelGrunnlag grunnlag = grunnlagBygger.byggGrunnlag(input);

        // Assert
        AktivitetIdentifikator forventetAktivitetIdentifikator = AktivitetIdentifikator.forArbeid(arbeidsforhold1.getArbeidsgiver().get().getIdentifikator(),
            arbeidsforhold1.getArbeidsforholdRef().getReferanse());

        List<AnnenpartUttakPeriode> uttakPerioderAnnenPart = grunnlag.getAnnenPart().getUttaksperioder();
        assertThat(uttakPerioderAnnenPart).hasSize(3);

        AnnenpartUttakPeriode annenPartGrunnlag = uttakPerioderAnnenPart.get(0);
        assertThat(annenPartGrunnlag.getFom()).isEqualTo(uttakMødrekvote.getFom());
        assertThat(annenPartGrunnlag.getTom()).isEqualTo(uttakMødrekvote.getTom());
        assertThat(annenPartGrunnlag.getAktiviteter()).hasSize(1);

        var aktivitetMødrekvote = annenPartGrunnlag.getAktiviteter().stream().findFirst().orElseThrow();
        assertThat(aktivitetMødrekvote.getAktivitetIdentifikator()).isEqualTo(forventetAktivitetIdentifikator);
        assertThat(aktivitetMødrekvote.getStønadskontotype()).isEqualTo(Stønadskontotype.MØDREKVOTE);
        assertThat(new Trekkdager(aktivitetMødrekvote.getTrekkdager().decimalValue()))
            .isEqualTo(uttakMødrekvote.getAktiviteter().get(0).getTrekkdager());

        AnnenpartUttakPeriode mappedFellesperiode = uttakPerioderAnnenPart.get(1);
        assertThat(mappedFellesperiode.getFom()).isEqualTo(uttakFellesperiode.getFom());
        assertThat(mappedFellesperiode.getTom()).isEqualTo(uttakFellesperiode.getTom());
        assertThat(mappedFellesperiode.getAktiviteter()).hasSize(1);

        var aktivitetFellesperiode = mappedFellesperiode.getAktiviteter().stream().findFirst().orElseThrow();
        assertThat(aktivitetFellesperiode.getAktivitetIdentifikator()).isEqualTo(forventetAktivitetIdentifikator);
        assertThat(aktivitetFellesperiode.getStønadskontotype()).isEqualTo(Stønadskontotype.FELLESPERIODE);
        assertThat(new Trekkdager(aktivitetFellesperiode.getTrekkdager().decimalValue()))
            .isEqualTo(uttakFellesperiode.getAktiviteter().get(0).getTrekkdager());
    }

    @Test
    public void testeSamtidigUttak() {
        String orgnr1 = OrgNummer.KUNSTIG_ORG;
        AktivitetIdentifikator aktivitet = AktivitetIdentifikator.forArbeid(orgnr1, InternArbeidsforholdRef.nyRef().getReferanse());

        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusDays(10);
        Arbeidsgiver virksomhet = lagVirksomhetArbeidsgiver(orgnr1);

        OppgittPeriodeEntitet oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medArbeidsgiver(virksomhet)
            .medErArbeidstaker(true)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(SamtidigUttaksprosent.TEN)
            .build();

        Behandling behandling = setupScenario(Collections.singletonList(oppgittPeriode));
        lagreYrkesAktiviter(behandling, virksomhet, Collections.singletonList(aktivitet.getArbeidsforholdId()),
            BigDecimal.valueOf(100), fom.minusYears(1));


        beregningsandelTjeneste.leggTilOrdinærtArbeid(virksomhet, InternArbeidsforholdRef.ref(aktivitet.getArbeidsforholdId()));

        var input = lagInput(behandling);
        RegelGrunnlag grunnlag = grunnlagBygger.byggGrunnlag(input);

        assertThat(grunnlag.getArbeid().getAktiviteter()).hasSize(1);

        var oppittePerioder = grunnlag.getSøknad().getOppgittePerioder();

        assertThat(oppittePerioder).isNotEmpty();
        assertThat(oppittePerioder.get(0).getSamtidigUttaksprosent()).isEqualTo(SamtidigUttaksprosent.TEN.decimalValue());
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, LocalDate.now());
    }

    private Behandling setup(OppgittPeriodeEntitet oppgittPeriode,
                             Arbeidsgiver arbeidsgiver,
                             BigDecimal stillingsprosent,
                             String arbeidsforholdId,
                             LocalDate startdatoArbeidsgiver) {
        return setup(Collections.singletonList(oppgittPeriode), arbeidsgiver, stillingsprosent, List.of(arbeidsforholdId), startdatoArbeidsgiver);
    }

    private Behandling setup(List<OppgittPeriodeEntitet> oppgittPeriode,
                             Arbeidsgiver arbeidsgiver,
                             BigDecimal stillingsprosent,
                             List<String> arbeidsforholdIdList,
                             LocalDate startdatoArbeidsgiver) {
        Behandling behandling = setupScenario(oppgittPeriode);
        lagreYrkesAktiviter(behandling, arbeidsgiver, arbeidsforholdIdList, stillingsprosent, startdatoArbeidsgiver);
        return behandling;
    }

    private Behandling setupFrilans(OppgittPeriodeEntitet oppgittPeriode, Arbeidsgiver arbeidsgiver) {
        Behandling behandling = setupScenario(Collections.singletonList(oppgittPeriode));
        lagreYrkesAktiviterFrilans(behandling, arbeidsgiver);
        return behandling;
    }

    private void lagreUttaksperiodegrense(Behandling behandling, UttaksperiodegrenseRepository repository) {
        Uttaksperiodegrense grense = new Uttaksperiodegrense.Builder(behandling.getBehandlingsresultat())
            .medFørsteLovligeUttaksdag(LocalDate.now().minusMonths(6)).medMottattDato(LocalDate.now().minusWeeks(2)).build();
        repository.lagre(behandling.getId(), grense);
    }

    private Arbeidsgiver lagVirksomhetArbeidsgiver(String orgnr) {
        return Arbeidsgiver.virksomhet(orgnr);
    }

    private void lagreStønadskontoer(Behandling behandling, FagsakRelasjonRepository fagsakRelasjonRepository) {
        Stønadskonto mødrekvote = Stønadskonto.builder().medStønadskontoType(StønadskontoType.MØDREKVOTE)
            .medMaxDager(30).build();
        Stønadskonto fellesperiode = Stønadskonto.builder().medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .medMaxDager(15).build();
        Stønadskonto fedrekvote = Stønadskonto.builder().medStønadskontoType(StønadskontoType.FEDREKVOTE)
            .medMaxDager(50).build();
        Stønadskonto foreldrepengerFørFødsel = Stønadskonto.builder().medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
            .medMaxDager(130).build();

        Stønadskontoberegning stønadskontoberegning = Stønadskontoberegning.builder()
            .medStønadskonto(mødrekvote)
            .medStønadskonto(fedrekvote)
            .medStønadskonto(fellesperiode)
            .medStønadskonto(foreldrepengerFørFødsel)
            .medRegelEvaluering(" ")
            .medRegelInput(" ")
            .build();

        fagsakRelasjonRepository.opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        fagsakRelasjonRepository.lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);
    }

    private void lagreYrkesAktiviter(Behandling behandling,
                                     Arbeidsgiver virksomhet,
                                     List<String> arbeidsforholdIdList,
                                     BigDecimal stillingsprosent,
                                     LocalDate fom) {
        InntektArbeidYtelseAggregatBuilder builder = lagYrkesAktiviter(behandling, virksomhet, arbeidsforholdIdList, stillingsprosent, fom);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private InntektArbeidYtelseAggregatBuilder lagYrkesAktiviter(Behandling behandling,
                                                                 Arbeidsgiver virksomhet,
                                                                 List<String> arbeidsforholdIdList,
                                                                 BigDecimal stillingsprosent,
                                                                 LocalDate fom) {
        InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());

        builder.leggTilAktørArbeid(aktørArbeidBuilder);

        for (var arbeidsforholdId : arbeidsforholdIdList) {
            YrkesaktivitetBuilder yrkesaktivitetBuilder = lagYrkesAktivitetForArbeidsforhold(virksomhet, stillingsprosent,
                fom, LocalDate.MAX,
                InternArbeidsforholdRef.ref(arbeidsforholdId),
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

            aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
            iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        }
        return builder;
    }

    private void lagreYrkesAktiviterFrilans(Behandling behandling, Arbeidsgiver virksomhet) {
        InntektArbeidYtelseAggregatBuilder builder = lagYrkesAktivitFrilans(behandling, virksomhet);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private InntektArbeidYtelseAggregatBuilder lagYrkesAktivitFrilans(Behandling behandling, Arbeidsgiver virksomhet) {
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER);

        LocalDate fraOgMed = LocalDate.now().minusWeeks(1);
        LocalDate tilOgMed = LocalDate.now().plusMonths(1);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        builder.leggTilAktørArbeid(aktørArbeidBuilder);

        YrkesaktivitetBuilder yrkesaktivitetBuilder = lagYrkesAktivitetForArbeidsforhold(virksomhet, BigDecimal.valueOf(100),
            fraOgMed, tilOgMed, null, ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER);

        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        YrkesaktivitetBuilder yrkesaktivitetBuilder2 = lagYrkesAktivitetForFrilansOverordnet(fraOgMed, tilOgMed, ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER);

        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder2);
        return builder;
    }

    private YrkesaktivitetBuilder lagYrkesAktivitetForFrilansOverordnet(LocalDate fraOgMed, LocalDate tilOgMed, ArbeidType arbeidType) {
        YrkesaktivitetBuilder yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed));
        yrkesaktivitetBuilder
            .medArbeidType(arbeidType)
            .leggTilAktivitetsAvtale(aktivitetsAvtale);

        return yrkesaktivitetBuilder;
    }

    private YrkesaktivitetBuilder lagYrkesAktivitetForArbeidsforhold(Arbeidsgiver virksomhet,
                                                                     BigDecimal stillingsprosent,
                                                                     LocalDate fraOgMed,
                                                                     LocalDate tilOgMed,
                                                                     InternArbeidsforholdRef arbeidsforholdId,
                                                                     ArbeidType arbeidType) {
        YrkesaktivitetBuilder yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());

        AktivitetsAvtaleBuilder aktivitetsAvtale = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed))
            .medProsentsats(stillingsprosent)
            .medAntallTimer(BigDecimal.valueOf(20.4d))
            .medAntallTimerFulltid(BigDecimal.valueOf(10.2d));
        AktivitetsAvtaleBuilder ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed));
        yrkesaktivitetBuilder
            .medArbeidType(arbeidType)
            .medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(arbeidsforholdId)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode);

        return yrkesaktivitetBuilder;
    }

    private Behandling setupScenario(List<OppgittPeriodeEntitet> oppgittPerioder) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(new OppgittFordelingEntitet(oppgittPerioder, true));
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        var førsteUttaksdag = oppgittPerioder.stream().min(Comparator.comparing(OppgittPeriodeEntitet::getFom)).get().getFom();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(førsteUttaksdag).build());

        Behandling behandling = lagre(scenario);
        lagreUttaksperiodegrense(behandling, repositoryProvider.getUttaksperiodegrenseRepository());
        lagreStønadskontoer(behandling, repositoryProvider.getFagsakRelasjonRepository());
        return behandling;
    }
}
