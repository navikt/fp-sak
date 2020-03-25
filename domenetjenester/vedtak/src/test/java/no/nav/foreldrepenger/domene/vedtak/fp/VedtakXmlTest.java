package no.nav.foreldrepenger.domene.vedtak.fp;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektPeriodeType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.vedtak.xml.BehandlingsresultatXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.FatteVedtakXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.PersonopplysningXmlFelles;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.Kjoenn;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.felles.testutilities.db.Repository;

@RunWith(CdiRunner.class)
public class VedtakXmlTest {

    private static final AktørId BRUKER_AKTØR_ID = AktørId.dummy();
    private static final Saksnummer SAKSNUMMER = new Saksnummer("12345");
    private static final AktørId ANNEN_PART_AKTØR_ID = AktørId.dummy();
    private static LocalDateTime VEDTAK_TIDSPUNKT = LocalDateTime.parse("2017-10-11T08:00");
    private static final IverksettingStatus IVERKSETTING_STATUS = IverksettingStatus.IKKE_IVERKSATT;
    private static final String ANSVARLIG_SAKSBEHANDLER = "fornavn etternavn";
    private static final String ORGNR = KUNSTIG_ORG;

    private static final LocalDate FØDSELSDATO_BARN = LocalDate.of(2017, Month.JANUARY, 1);
    private static final LocalDate FØRSTE_UTTAKSDATO_OPPGITT = LocalDate.now().minusDays(20);
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static final BigDecimal MÅNEDSBELØP_TILSTØTENDE_YTELSE = BigDecimal.valueOf(10000L);

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final BeregningsresultatRepository beregningsresultatRepository = new BeregningsresultatRepository(repoRule.getEntityManager());

    @Inject
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;


    private Repository repository = repoRule.getRepository();

    @Mock
    private TpsTjeneste tpsTjeneste;

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    private KodeverkRepository kodeverkRepository;

    @Inject
    private BehandlingsresultatXmlTjeneste behandlingsresultatXmlTjeneste;

    private PersonopplysningXmlTjenesteImpl personopplysningXmlTjeneste;

    private FatteVedtakXmlTjeneste fpSakVedtakXmlTjeneste;

    private VedtakXmlTjeneste vedtakXmlTjeneste;

    @Before
    public void oppsett() {
        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        Mockito.when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.any())).thenReturn(stp);
        var poXmlFelles = new PersonopplysningXmlFelles(tpsTjeneste, kodeverkRepository);
        personopplysningXmlTjeneste = new PersonopplysningXmlTjenesteImpl(poXmlFelles, repositoryProvider, kodeverkRepository, personopplysningTjeneste,
            iayTjeneste, ytelseFordelingTjeneste, mock(VergeRepository.class), mock(VirksomhetRepository.class));
        vedtakXmlTjeneste = new VedtakXmlTjeneste(repositoryProvider);
        fpSakVedtakXmlTjeneste = new FatteVedtakXmlTjeneste(repositoryProvider, vedtakXmlTjeneste, new UnitTestLookupInstanceImpl<>(personopplysningXmlTjeneste),
            behandlingsresultatXmlTjeneste,
            skjæringstidspunktTjeneste);
    }

    @Test
    public void test_konvertering_kjønn() {
        Kjoenn søkersKjønn = Kjoenn.KVINNE;
        NavBrukerKjønn navBrukerKjønn = NavBrukerKjønn.fraKode(søkersKjønn.getKode());
        assertThat(navBrukerKjønn).isEqualTo(NavBrukerKjønn.KVINNE);
    }

    @Test
    public void skal_opprette_vedtaks_xml() {
        Behandling behandling = byggBehandlingMedVedtak();
        String avkortetXmlElement = "avkortet>";

        // Act
        String xml = fpSakVedtakXmlTjeneste.opprettVedtakXml(behandling.getId());

        // Assert
        assertNotNull(xml);
        assertThat(xml).contains(avkortetXmlElement);
    }

    private Behandling byggBehandlingMedVedtak() {
        String selvstendigNæringsdrivendeOrgnr = KUNSTIG_ORG;

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBruker(BRUKER_AKTØR_ID, NavBrukerKjønn.KVINNE)
            .medSaksnummer(SAKSNUMMER);
        scenario.medDefaultOppgittDekningsgrad();
        scenario.medSøknadAnnenPart().medAktørId(ANNEN_PART_AKTØR_ID);
        scenario.medSøknadHendelse()
            .medFødselsDato(FØDSELSDATO_BARN);

        scenario.medFordeling(opprettOppgittFordeling());
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        PerioderAleneOmsorgEntitet perioderAleneOmsorg = new PerioderAleneOmsorgEntitet();
        perioderAleneOmsorg.leggTil(new PeriodeAleneOmsorgEntitet(LocalDate.now(), LocalDate.now().plusDays(10)));
        scenario.medPeriodeMedAleneomsorg(perioderAleneOmsorg);

        Behandling behandling = lagre(scenario);
        Behandlingsresultat behandlingsresultat = opprettBehandlingsresultatMedVilkårResultatForBehandling(behandling);
        repository.lagre(behandlingsresultat);
        repository.flushAndClear();

        mockTidligereYtelse(behandling, RelatertYtelseType.SYKEPENGER, null, Arbeidskategori.ARBEIDSTAKER, selvstendigNæringsdrivendeOrgnr);

        Uttaksperiodegrense uttaksperiodegrense = new Uttaksperiodegrense.Builder(behandling.getBehandlingsresultat())
            .medFørsteLovligeUttaksdag(LocalDate.now())
            .medMottattDato(LocalDate.now())
            .build();
        repositoryProvider.getUttakRepository().lagreUttaksperiodegrense(behandling.getId(), uttaksperiodegrense);

        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(), LocalDate.now().plusDays(11))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        UttakResultatPerioderEntitet uttakResultatPerioder1 = new UttakResultatPerioderEntitet();

        UttakResultatPeriodeEntitet uttakResultatPeriode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(),
            LocalDate.now().plusMonths(3))
                .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                .build();

        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.virksomhet(selvstendigNæringsdrivendeOrgnr), InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();

        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = UttakResultatPeriodeAktivitetEntitet.builder(uttakResultatPeriode,
            uttakAktivitet)
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .medTrekkdager(new Trekkdager(10))
            .medArbeidsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(null) // PFP-4396 tester at utbetalingsgrad kan være null
            .build();

        periode.leggTilAktivitet(periodeAktivitet);

        uttakResultatPerioder1.leggTilPeriode(periode);

        repositoryProvider.getUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakResultatPerioder1);

        opprettStønadskontoer(behandling);
        lagBeregningsgrunnlag(behandling);

        BeregningsresultatEntitet beregningsresultat = lagBeregningsresultatFP();
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        BehandlingVedtak vedtak = BehandlingVedtak.builder()
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medIverksettingStatus(IVERKSETTING_STATUS)
            .medVedtakstidspunkt(VEDTAK_TIDSPUNKT)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medBehandlingsresultat(behandlingsresultat)
            .build();
        behandlingVedtakRepository.lagre(vedtak, behandlingRepository.taSkriveLås(behandling));

        Fagsak fagsakMora = scenario.getFagsak();
        Fagsak fagsakForFar = FagsakBuilder.nyForeldrepengesak(RelasjonsRolleType.FARA).build();
        repositoryProvider.getFagsakRepository().opprettNy(fagsakForFar);

        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(fagsakMora, fagsakForFar, behandling);
        repoRule.getRepository().flushAndClear();

        return behandling;
    }

    private void lagBeregningsgrunnlag(Behandling behandling) {
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();
        BeregningsgrunnlagPeriode bgPeriode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null)
            .build(bg);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.FRILANSER)
            .medAvkortetPrÅr(BigDecimal.TEN)
            .medAvkortetBrukersAndelPrÅr(BigDecimal.TEN)
            .medAvkortetRefusjonPrÅr(BigDecimal.ZERO)
            .medRedusertPrÅr(BigDecimal.TEN)
            .medRedusertBrukersAndelPrÅr(BigDecimal.TEN)
            .medRedusertRefusjonPrÅr(BigDecimal.ZERO)
            .build(bgPeriode);

        beregningsgrunnlagRepository.lagre(behandling.getId(), bg, BeregningsgrunnlagTilstand.FASTSATT);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    private void mockTidligereYtelse(Behandling behandling, RelatertYtelseType relatertYtelseType, BigDecimal prosent, Arbeidskategori arbeidskategori,
                                     String virksomhetOrgnr) {
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(),
            VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder ytelserBuilder = inntektArbeidYtelseAggregatBuilder.getAktørYtelseBuilder(BRUKER_AKTØR_ID);

        YtelseBuilder ytelseBuilder = ytelserBuilder.getYtelselseBuilderForType(Fagsystem.FPSAK, relatertYtelseType, SAKSNUMMER)
            .medStatus(RelatertYtelseTilstand.AVSLUTTET)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusDays(6), SKJÆRINGSTIDSPUNKT.plusDays(8)))
            .medKilde(Fagsystem.INFOTRYGD);

        YtelseStørrelse ytelseStørrelse = YtelseStørrelseBuilder.ny()
            .medBeløp(MÅNEDSBELØP_TILSTØTENDE_YTELSE)
            .medHyppighet(InntektPeriodeType.MÅNEDLIG)
            .medVirksomhet(virksomhetOrgnr)
            .build();
        YtelseGrunnlagBuilder ytelseGrunnlagBuilder = ytelseBuilder.getGrunnlagBuilder()
            .medArbeidskategori(arbeidskategori)
            .medYtelseStørrelse(ytelseStørrelse);

        if (RelatertYtelseType.FORELDREPENGER.equals(relatertYtelseType)) {
            ytelseGrunnlagBuilder.medDekningsgradProsent(prosent);
        }
        if (RelatertYtelseType.SYKEPENGER.equals(relatertYtelseType)) {
            ytelseGrunnlagBuilder.medInntektsgrunnlagProsent(prosent);
        }
        YtelseGrunnlag ytelseGrunnlag = ytelseGrunnlagBuilder
            .build();
        ytelseBuilder.medYtelseGrunnlag(ytelseGrunnlag);

        ytelserBuilder.leggTilYtelse(ytelseBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørYtelse(ytelserBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);

    }

    private BeregningsresultatEntitet lagBeregningsresultatFP() {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
        BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(FØRSTE_UTTAKSDATO_OPPGITT, FØRSTE_UTTAKSDATO_OPPGITT.plusWeeks(2))
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(123)
            .medDagsatsFraBg(123)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(beregningsresultatPeriode);
        return beregningsresultat;
    }

    private Behandlingsresultat opprettBehandlingsresultatMedVilkårResultatForBehandling(Behandling behandling) {

        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builderEndreEksisterende(behandling.getBehandlingsresultat())
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
            .buildFor(behandling);
        VilkårResultat vilkårResultat = VilkårResultat.builder().medVilkårResultatType(VilkårResultatType.INNVILGET)
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .buildFor(behandlingsresultat);
        repository.lagre(vilkårResultat);
        behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
        behandling.setBehandlingresultat(behandlingsresultat);

        return behandlingsresultat;
    }

    private void opprettStønadskontoer(Behandling behandling) {
        Stønadskonto foreldrepengerFørFødsel = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
            .medMaxDager(15)
            .build();
        Stønadskonto mødrekvote = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.MØDREKVOTE)
            .medMaxDager(50)
            .build();
        Stønadskonto fellesperiode = Stønadskonto.builder()
            .medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .medMaxDager(50)
            .build();
        Stønadskontoberegning stønadskontoberegning = Stønadskontoberegning.builder()
            .medRegelEvaluering("evaluering")
            .medRegelInput("grunnlag")
            .medStønadskonto(mødrekvote).medStønadskonto(fellesperiode).medStønadskonto(foreldrepengerFørFødsel).build();

        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);
    }

    private OppgittFordelingEntitet opprettOppgittFordeling() {
        OppgittPeriodeBuilder periode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medPeriode(FØRSTE_UTTAKSDATO_OPPGITT, FØRSTE_UTTAKSDATO_OPPGITT.plusWeeks(2))
            .medArbeidsgiver(opprettOgLagreArbeidsgiver(ORGNR));

        return new OppgittFordelingEntitet(singletonList(periode.build()), true);
    }

    private Arbeidsgiver opprettOgLagreArbeidsgiver(String orgNr) {
        return Arbeidsgiver.virksomhet(orgNr);
    }

    private static BehandlingReferanse lagReferanse(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }
}
