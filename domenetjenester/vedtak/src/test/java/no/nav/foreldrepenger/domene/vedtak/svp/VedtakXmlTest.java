package no.nav.foreldrepenger.domene.vedtak.svp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

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
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.PeriodeÅrsak;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.vedtak.xml.BehandlingsresultatXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.FatteVedtakXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.PersonopplysningXmlFelles;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.felles.testutilities.db.Repository;

@RunWith(CdiRunner.class) // TODO replace
public class VedtakXmlTest {

    private static final String ORGNR = KUNSTIG_ORG;
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusDays(5);
    private static final IverksettingStatus IVERKSETTING_STATUS = IverksettingStatus.IKKE_IVERKSATT;
    private static final String ANSVARLIG_SAKSBEHANDLER = "fornavn etternavn";
    private static final BeregningsgrunnlagTilstand STEG_FASTSATT = BeregningsgrunnlagTilstand.FASTSATT;
    private static LocalDateTime VEDTAK_DATO = LocalDateTime.parse("2017-10-11T08:00");
    private final AktørId AKTØR_ID = AktørId.dummy();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private LocalDate jordmorsdato = LocalDate.now().minusDays(30);
    private final EntityManager entityManager = repoRule.getEntityManager();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final SvangerskapspengerRepository svangerskapspengerRepository = repositoryProvider.getSvangerskapspengerRepository();
    private Repository repository = repoRule.getRepository();

    @Mock
    private PersoninfoAdapter personinfoAdapter;

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    private BehandlingsresultatXmlTjeneste behandlingsresultatXmlTjeneste;

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste = Mockito.mock(HentOgLagreBeregningsgrunnlagTjeneste.class);

    @Inject
    private BeregningsresultatRepository beregningsresultatRepository;

    private PersonopplysningXmlTjenesteImpl personopplysningXmlTjeneste;
    private FatteVedtakXmlTjeneste fpSakVedtakXmlTjeneste;

    private VedtakXmlTjeneste vedtakXmlTjeneste;

    @Before
    public void oppsett() {
        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        Mockito.when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.any())).thenReturn(stp);

        var poXmlFelles = new PersonopplysningXmlFelles(personinfoAdapter);
        personopplysningXmlTjeneste = new PersonopplysningXmlTjenesteImpl(poXmlFelles, repositoryProvider, personopplysningTjeneste,
            iayTjeneste, ytelseFordelingTjeneste, mock(VergeRepository.class), mock(VirksomhetTjeneste.class));
        vedtakXmlTjeneste = new VedtakXmlTjeneste(repositoryProvider);
        fpSakVedtakXmlTjeneste = new FatteVedtakXmlTjeneste(repositoryProvider, vedtakXmlTjeneste,
            new UnitTestLookupInstanceImpl<>(personopplysningXmlTjeneste),
            behandlingsresultatXmlTjeneste,
            skjæringstidspunktTjeneste);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_opprette_vedtaks_xml() {

        // Arrange
        ScenarioMorSøkerSvangerskapspenger scenario = byggBehandlingMedMorSøkerSVP();
        scenario.medBruker(AKTØR_ID, NavBrukerKjønn.KVINNE);
        Behandling behandling = lagre(scenario);
        lagreUttak(behandling);
        lagreTilrettelegging(behandling);
        lagreSvp(behandling, jordmorsdato);

        Behandlingsresultat behandlingsresultat = opprettBehandlingsresultatMedVilkårResultatForBehandling(behandling);

        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(true);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        BehandlingVedtak vedtak = BehandlingVedtak.builder()
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medIverksettingStatus(IVERKSETTING_STATUS)
            .medVedtakstidspunkt(VEDTAK_DATO)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medBehandlingsresultat(behandlingsresultat)
            .build();
        behandlingVedtakRepository.lagre(vedtak, behandlingRepository.taSkriveLås(behandling));

        BeregningsgrunnlagEntitet beregningsgrunnlag = buildBeregningsgrunnlag();

        var beregningsgrunnlagGrunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty()).medBeregningsgrunnlag(beregningsgrunnlag)
            .build(behandling.getId(), STEG_FASTSATT);
        Mockito.when(beregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(Optional.of(beregningsgrunnlagGrunnlag));

        // Act

        String xml = fpSakVedtakXmlTjeneste.opprettVedtakXml(behandling.getId());

        assertNotNull(xml);

    }

    void lagreTilrettelegging(Behandling behandling) {
        var jordmorsDato = LocalDate.of(2019, Month.APRIL, 1);
        var tilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(jordmorsDato)
            .medIngenTilrettelegging(jordmorsDato)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medKopiertFraTidligereBehandling(false)
            .medMottattTidspunkt(LocalDateTime.now())
            .build();
        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
            .build();
        repositoryProvider.getSvangerskapspengerRepository().lagreOgFlush(svpGrunnlag);
    }

    public void lagreUttak(Behandling behandling) {

        var fom = LocalDate.of(2019, Month.JANUARY, 1);
        var tom = LocalDate.of(2019, Month.MARCH, 31);

        var uttakPeriodeA = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(fom, tom)
            .medRegelInput("{}")
            .medRegelEvaluering("{}")
            .medUtbetalingsgrad(BigDecimal.valueOf(30L))
            .medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak.INGEN)
            .medPeriodeResultatType(PeriodeResultatType.INNVILGET)
            .build();

        var uttakPeriodeB = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(tom.plusDays(1), tom.plusDays(10))
            .medRegelInput("{}")
            .medRegelEvaluering("{}")
            .medUtbetalingsgrad(BigDecimal.valueOf(50L))
            .medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak.INGEN)
            .medPeriodeResultatType(PeriodeResultatType.AVSLÅTT)
            .build();

        var uttakPeriodeC = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(tom.plusDays(11), tom.plusDays(20))
            .medRegelInput("{}")
            .medRegelEvaluering("{}")
            .medUtbetalingsgrad(BigDecimal.valueOf(20L))
            .medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak.INGEN)
            .medPeriodeResultatType(PeriodeResultatType.MANUELL_BEHANDLING)
            .build();

        var uttakArbeidsforholdA = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.person(AktørId.dummy()), InternArbeidsforholdRef.nyRef())
            .medPeriode(uttakPeriodeA)
            .medPeriode(uttakPeriodeB)
            .medPeriode(uttakPeriodeC)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();

        var uttakArbeidsforholdB = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.person(AktørId.dummy()), InternArbeidsforholdRef.nyRef())
            .medPeriode(uttakPeriodeA)
            .medPeriode(uttakPeriodeB)
            .medPeriode(uttakPeriodeC)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();

        var uttakArbeidsforholdC = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.person(AktørId.dummy()), InternArbeidsforholdRef.nyRef())
            .medPeriode(uttakPeriodeA)
            .medPeriode(uttakPeriodeB)
            .medPeriode(uttakPeriodeC)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();

        var uttakResultat = new SvangerskapspengerUttakResultatEntitet.Builder(behandling.getBehandlingsresultat())
            .medUttakResultatArbeidsforhold(uttakArbeidsforholdA).medUttakResultatArbeidsforhold(uttakArbeidsforholdB)
            .medUttakResultatArbeidsforhold(uttakArbeidsforholdC).build();
        repositoryProvider.getSvangerskapspengerUttakResultatRepository().lagre(behandling.getId(), uttakResultat);

        entityManager.flush();
        entityManager.clear();

    }

    private ScenarioMorSøkerSvangerskapspenger byggBehandlingMedMorSøkerSVP() {
        ScenarioMorSøkerSvangerskapspenger scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();

        scenario.medBruker(AKTØR_ID, NavBrukerKjønn.KVINNE);
        leggTilSøker(scenario);

        return scenario;
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario) {
        PersonInformasjon.Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, NavBrukerKjønn.KVINNE, Region.UDEFINERT)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }

    private void lagreSvp(Behandling behandling, LocalDate jordmorsdato) {
        SvpTilretteleggingEntitet tilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(jordmorsdato)
            .medIngenTilrettelegging(jordmorsdato)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medMottattTidspunkt(LocalDateTime.now())
            .medKopiertFraTidligereBehandling(false)
            .build();
        SvpGrunnlagEntitet svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
            .build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
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

    private BeregningsresultatAndel buildBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode, Boolean brukerErMottaker, int dagsats) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(brukerErMottaker)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
            .medDagsats(dagsats)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.ZERO)
            .medDagsatsFraBg(dagsats)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(beregningsresultatPeriode);
    }

    private BeregningsresultatPeriode buildBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat, int fom, int tom) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.now().plusDays(fom), LocalDate.now().plusDays(tom))
            .build(beregningsresultat);
    }

    private BeregningsresultatEntitet buildBeregningsresultatFP(Boolean brukerErMottaker) {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        BeregningsresultatPeriode brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode1, brukerErMottaker, 2160);
        if (!brukerErMottaker) {
            buildBeregningsresultatAndel(brPeriode1, true, 0);
        }
        BeregningsresultatPeriode brPeriode2 = buildBeregningsresultatPeriode(beregningsresultat, 21, 28);
        buildBeregningsresultatAndel(brPeriode2, brukerErMottaker, 2160);
        if (!brukerErMottaker) {
            buildBeregningsresultatAndel(brPeriode2, true, 0);
        }
        return beregningsresultat;
    }

    private BeregningsgrunnlagPeriode buildBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(15))
            .medBruttoPrÅr(BigDecimal.valueOf(534343.55))
            .medAvkortetPrÅr(BigDecimal.valueOf(223421.33))
            .medRedusertPrÅr(BigDecimal.valueOf(23412.32))
            .medRegelEvalueringForeslå("input1", "clob1")
            .medRegelEvalueringFastsett("input2", "clob2")
            .leggTilPeriodeÅrsak(PeriodeÅrsak.UDEFINERT)
            .build(beregningsgrunnlag);
    }

    private BeregningsgrunnlagEntitet buildBeregningsgrunnlag() {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medGrunnbeløp(BigDecimal.valueOf(91425))
            .medRegelloggSkjæringstidspunkt("input1", "clob1")
            .medRegelloggBrukersStatus("input2", "clob2")
            .medRegelinputPeriodisering("input3")
            .build();
        buildSammenligningsgrunnlag(beregningsgrunnlag);
        buildBgAktivitetStatus(beregningsgrunnlag);
        BeregningsgrunnlagPeriode bgPeriode = buildBeregningsgrunnlagPeriode(beregningsgrunnlag);
        buildBgPrStatusOgAndel(bgPeriode);
        return beregningsgrunnlag;
    }

    private BeregningsgrunnlagAktivitetStatus buildBgAktivitetStatus(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build(beregningsgrunnlag);
    }

    private Sammenligningsgrunnlag buildSammenligningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return Sammenligningsgrunnlag.builder()
            .medSammenligningsperiode(LocalDate.now().minusDays(12), LocalDate.now().minusDays(6))
            .medRapportertPrÅr(BigDecimal.valueOf(323212.12))
            .medAvvikPromille(BigDecimal.valueOf(120L))
            .build(beregningsgrunnlag);
    }

    private BeregningsgrunnlagPrStatusOgAndel buildBgPrStatusOgAndel(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold
            .builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
            .medNaturalytelseBortfaltPrÅr(BigDecimal.valueOf(3232.32))
            .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
            .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
        return BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga)
            .medAktivitetStatus(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medBeregningsperiode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5))
            .medOverstyrtPrÅr(BigDecimal.valueOf(4444432.32))
            .medAvkortetPrÅr(BigDecimal.valueOf(423.23))
            .medRedusertPrÅr(BigDecimal.valueOf(52335))
            .build(beregningsgrunnlagPeriode);
    }
}
