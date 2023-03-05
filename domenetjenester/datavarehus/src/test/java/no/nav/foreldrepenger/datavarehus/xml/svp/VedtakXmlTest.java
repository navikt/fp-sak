package no.nav.foreldrepenger.datavarehus.xml.svp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

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
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.datavarehus.xml.BehandlingsresultatXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.FatteVedtakXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.PersonopplysningXmlFelles;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlTjeneste;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.entiteter.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagPeriodeRegelType;
import no.nav.foreldrepenger.domene.modell.kodeverk.PeriodeÅrsak;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@CdiDbAwareTest
class VedtakXmlTest {

    private static final String ORGNR = KUNSTIG_ORG;
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusDays(5);
    private static final IverksettingStatus IVERKSETTING_STATUS = IverksettingStatus.IKKE_IVERKSATT;
    private static final String ANSVARLIG_SAKSBEHANDLER = "fornavn etternavn";
    private static final LocalDateTime VEDTAK_DATO = LocalDateTime.parse("2017-10-11T08:00");
    private final AktørId AKTØR_ID = AktørId.dummy();

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private EntityManager entityManager;

    private final LocalDate jordmorsdato = LocalDate.now().minusDays(30);

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

    @Inject
    private BeregningsresultatRepository beregningsresultatRepository;

    private FatteVedtakXmlTjeneste fpSakVedtakXmlTjeneste;

    @BeforeEach
    public void oppsett(EntityManager em) {
        entityManager = em;
        repositoryProvider = new BehandlingRepositoryProvider(em);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        svangerskapspengerRepository = new SvangerskapspengerRepository(em);
        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        Mockito.when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.any())).thenReturn(stp);

        var poXmlFelles = new PersonopplysningXmlFelles(personinfoAdapter);
        var personopplysningXmlTjeneste = new PersonopplysningXmlTjenesteImpl(poXmlFelles,
            repositoryProvider, personopplysningTjeneste, iayTjeneste, ytelseFordelingTjeneste,
            mock(VergeRepository.class), mock(VirksomhetTjeneste.class));
        var vedtakXmlTjeneste = new VedtakXmlTjeneste(repositoryProvider);
        fpSakVedtakXmlTjeneste = new FatteVedtakXmlTjeneste(repositoryProvider, vedtakXmlTjeneste,
                new UnitTestLookupInstanceImpl<>(personopplysningXmlTjeneste),
                behandlingsresultatXmlTjeneste,
                skjæringstidspunktTjeneste);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    void skal_opprette_vedtaks_xml(EntityManager em) {

        // Arrange
        var scenario = byggBehandlingMedMorSøkerSVP();
        scenario.medBruker(AKTØR_ID, NavBrukerKjønn.KVINNE);
        var behandling = lagre(scenario);
        lagreUttak(behandling, em);
        lagreTilrettelegging(behandling);
        lagreSvp(behandling, jordmorsdato);

        var behandlingsresultat = opprettBehandlingsresultatMedVilkårResultatForBehandling(behandling);

        var beregningsresultat = buildBeregningsresultatFP(true);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        var behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        var vedtak = BehandlingVedtak.builder()
                .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
                .medIverksettingStatus(IVERKSETTING_STATUS)
                .medVedtakstidspunkt(VEDTAK_DATO)
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medBehandlingsresultat(behandlingsresultat)
                .build();
        behandlingVedtakRepository.lagre(vedtak, behandlingRepository.taSkriveLås(behandling));
        var xml = fpSakVedtakXmlTjeneste.opprettVedtakXml(behandling.getId());
        assertThat(xml).isNotNull();

    }

    void lagreTilrettelegging(Behandling behandling) {
        var jordmorsDato = LocalDate.of(2019, Month.APRIL, 1);
        var tilrettelegging = new SvpTilretteleggingEntitet.Builder()
                .medBehovForTilretteleggingFom(jordmorsDato)
                .medIngenTilrettelegging(jordmorsDato, jordmorsDato)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
                .medKopiertFraTidligereBehandling(false)
                .medMottattTidspunkt(LocalDateTime.now())
                .build();
        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
                .medBehandlingId(behandling.getId())
                .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
                .build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
    }

    public void lagreUttak(Behandling behandling, EntityManager entityManager) {

        var fom = LocalDate.of(2019, Month.JANUARY, 1);
        var tom = LocalDate.of(2019, Month.MARCH, 31);

        var uttakPeriodeA = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(fom, tom)
                .medRegelInput("{}")
                .medRegelEvaluering("{}")
                .medUtbetalingsgrad(new Utbetalingsgrad(30L))
                .medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak.INGEN)
                .medPeriodeResultatType(PeriodeResultatType.INNVILGET)
                .build();

        var uttakPeriodeB = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(tom.plusDays(1), tom.plusDays(10))
                .medRegelInput("{}")
                .medRegelEvaluering("{}")
                .medUtbetalingsgrad(new Utbetalingsgrad(50L))
                .medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak.INGEN)
                .medPeriodeResultatType(PeriodeResultatType.AVSLÅTT)
                .build();

        var uttakPeriodeC = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(tom.plusDays(11), tom.plusDays(20))
                .medRegelInput("{}")
                .medRegelEvaluering("{}")
                .medUtbetalingsgrad(new Utbetalingsgrad(20L))
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
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();

        scenario.medBruker(AKTØR_ID, NavBrukerKjønn.KVINNE);
        leggTilSøker(scenario);

        return scenario;
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario) {
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();
        var søker = builderForRegisteropplysninger
                .medPersonas()
                .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, NavBrukerKjønn.KVINNE)
                .build();
        scenario.medRegisterOpplysninger(søker);
    }

    private void lagreSvp(Behandling behandling, LocalDate jordmorsdato) {
        var tilrettelegging = new SvpTilretteleggingEntitet.Builder()
                .medBehovForTilretteleggingFom(jordmorsdato)
                .medIngenTilrettelegging(jordmorsdato, jordmorsdato)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
                .medMottattTidspunkt(LocalDateTime.now())
                .medKopiertFraTidligereBehandling(false)
                .build();
        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
                .medBehandlingId(behandling.getId())
                .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
                .build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
    }

    private Behandlingsresultat opprettBehandlingsresultatMedVilkårResultatForBehandling(Behandling behandling) {

        var behandlingsresultat = Behandlingsresultat.builderEndreEksisterende(behandling.getBehandlingsresultat())
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
                .buildFor(behandling);

        var vilkårResultat = VilkårResultat.builder().medVilkårResultatType(VilkårResultatType.INNVILGET)
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .buildFor(behandlingsresultat);

        entityManager.persist(vilkårResultat);
        behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
        behandling.setBehandlingresultat(behandlingsresultat);

        return behandlingsresultat;
    }

    private BeregningsresultatAndel buildBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode, Boolean brukerErMottaker,
            int dagsats) {
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
        var beregningsresultat = BeregningsresultatEntitet.builder()
                .medRegelInput("clob1")
                .medRegelSporing("clob2")
                .build();
        var brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode1, brukerErMottaker, 2160);
        if (!brukerErMottaker) {
            buildBeregningsresultatAndel(brPeriode1, true, 0);
        }
        var brPeriode2 = buildBeregningsresultatPeriode(beregningsresultat, 21, 28);
        buildBeregningsresultatAndel(brPeriode2, brukerErMottaker, 2160);
        if (!brukerErMottaker) {
            buildBeregningsresultatAndel(brPeriode2, true, 0);
        }
        return beregningsresultat;
    }

    private BeregningsgrunnlagPeriode buildBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return BeregningsgrunnlagPeriode.ny()
                .medBeregningsgrunnlagPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(15))
                .medBruttoPrÅr(BigDecimal.valueOf(534343.55))
                .medAvkortetPrÅr(BigDecimal.valueOf(223421.33))
                .medRedusertPrÅr(BigDecimal.valueOf(23412.32))
                .medRegelEvaluering("input1", "clob1", BeregningsgrunnlagPeriodeRegelType.FORESLÅ)
                .medRegelEvaluering("input2", "clob2", BeregningsgrunnlagPeriodeRegelType.FASTSETT)
                .leggTilPeriodeÅrsak(PeriodeÅrsak.UDEFINERT)
                .build(beregningsgrunnlag);
    }

    private BeregningsgrunnlagEntitet buildBeregningsgrunnlag() {
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
                .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
                .medGrunnbeløp(BigDecimal.valueOf(91425))
                .medRegelloggSkjæringstidspunkt("input1", "clob1")
                .medRegelloggBrukersStatus("input2", "clob2")
                .medRegelinputPeriodisering("input3")
                .build();
        buildSammenligningsgrunnlag(beregningsgrunnlag);
        buildBgAktivitetStatus(beregningsgrunnlag);
        var bgPeriode = buildBeregningsgrunnlagPeriode(beregningsgrunnlag);
        buildBgPrStatusOgAndel(bgPeriode);
        return beregningsgrunnlag;
    }

    private BeregningsgrunnlagAktivitetStatus buildBgAktivitetStatus(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return BeregningsgrunnlagAktivitetStatus.builder()
                .medAktivitetStatus(no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
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
        var bga = BGAndelArbeidsforhold
                .builder()
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medNaturalytelseBortfaltPrÅr(BigDecimal.valueOf(3232.32))
                .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
                .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
        return BeregningsgrunnlagPrStatusOgAndel.builder()
                .medBGAndelArbeidsforhold(bga)
                .medAktivitetStatus(no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
                .medBeregningsperiode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5))
                .medOverstyrtPrÅr(BigDecimal.valueOf(4444432.32))
                .medAvkortetPrÅr(BigDecimal.valueOf(423.23))
                .medRedusertPrÅr(BigDecimal.valueOf(52335))
                .build(beregningsgrunnlagPeriode);
    }
}
