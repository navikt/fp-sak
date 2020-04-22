package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import static no.nav.folketrygdloven.kalkulator.adapter.vltilregelmodell.MapBeregningAktiviteterFraVLTilRegel.INGEN_AKTIVITET_MELDING;
import static no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.fra_kalkulus.KalkulusTilBehandlingslagerMapper.mapBeregningsgrunnlag;
import static no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.fra_kalkulus.KalkulusTilBehandlingslagerMapper.mapSaksbehandletAktivitet;
import static no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper.mapSaksbehandletAktivitet;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.Grunnbeløp;
import no.nav.folketrygdloven.kalkulator.BeregningsperiodeTjeneste;
import no.nav.folketrygdloven.kalkulator.FastsettBeregningAktiviteter;
import no.nav.folketrygdloven.kalkulator.FastsettSkjæringstidspunktOgStatuser;
import no.nav.folketrygdloven.kalkulator.adapter.regelmodelltilvl.MapBGSkjæringstidspunktOgStatuserFraRegelTilVL;
import no.nav.folketrygdloven.kalkulator.adapter.vltilregelmodell.MapBeregningAktiviteterFraVLTilRegel;
import no.nav.folketrygdloven.kalkulator.gradering.AktivitetGradering;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.fp.GrunnbeløpTjenesteImplFP;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.IAYMapperTilKalkulus;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.MapBehandlingRef;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.OpptjeningMapperTilKalkulus;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.testutilities.behandling.ScenarioForeldrepenger;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektPeriodeType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;


public class FastsettBeregningAktiviteterOgStatuserTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.APRIL, 10);
    private static final LocalDate FØRSTE_UTTAKSDAG = SKJÆRINGSTIDSPUNKT_OPPTJENING.plusYears(1);
    private static final LocalDate DAGEN_FØR_SFO = SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1);

    private static final String ORG_NUMMER = "915933149";
    private static final String ORG_NUMMER2 = "974760673";
    private static final String ORG_NUMMER_MED_FLERE_ARBEIDSFORHOLD = ORG_NUMMER;
    public static final List<Grunnbeløp> GRUNNBELØP = List.of(new Grunnbeløp(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(5),
        SKJÆRINGSTIDSPUNKT_OPPTJENING.plusYears(5), 99_000L, 99_000L));

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final EntityManager entityManager = repoRule.getEntityManager();
    private RepositoryProvider repositoryProvider = new RepositoryProvider(entityManager);

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private BehandlingReferanse behandlingReferanse;

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
    private GrunnbeløpTjeneste grunnbeløpTjeneste;
    private BeregningIAYTestUtil iayTestUtil;

    private FastsettBeregningAktiviteter fastsettBeregningAktiviteter = new FastsettBeregningAktiviteter(new UnitTestLookupInstanceImpl<>(new MapBeregningAktiviteterFraVLTilRegel()));
    private FastsettSkjæringstidspunktOgStatuser fastsettSkjæringstidspunktOgStatuser = new FastsettSkjæringstidspunktOgStatuser(
        new MapBGSkjæringstidspunktOgStatuserFraRegelTilVL(new UnitTestLookupInstanceImpl<>(new BeregningsperiodeTjeneste())));

    private final AtomicLong journalpostIdInc = new AtomicLong(123L);
    private AbstractTestScenario<?> scenario;

    @Before
    public void setup() {
        grunnbeløpTjeneste = new GrunnbeløpTjenesteImplFP(beregningsgrunnlagRepository, 2);
        iayTestUtil = new BeregningIAYTestUtil(iayTjeneste);
        scenario = ScenarioForeldrepenger.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
    }

    @Test
    public void testForIngenOpptjeningsaktiviteter_exception() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(2), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(13), arbId1);

        // Assert
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(CoreMatchers.containsString(INGEN_AKTIVITET_MELDING));

        // Act
        act();
    }

    private BeregningsgrunnlagEntitet act() {
        return act(new OpptjeningAktiviteter());
    }

    private BeregningsgrunnlagEntitet act(OpptjeningAktiviteter opptjeningAktiviteter) {
        return act(opptjeningAktiviteter, List.of());
    }

    private BeregningsgrunnlagEntitet act(OpptjeningAktiviteter opptjeningAktiviteter, Collection<Inntektsmelding> inntektsmeldinger) {
        var ref = lagReferanseMedStp();
        var input = lagBeregningsgrunnlagInput(ref, opptjeningAktiviteter, inntektsmeldinger);
        var beregningAktivitetAggregat = fastsettBeregningAktiviteter.fastsettAktiviteter(input);
        return mapBeregningsgrunnlag(fastsettSkjæringstidspunktOgStatuser.fastsett(input, beregningAktivitetAggregat, input.getIayGrunnlag(),
            grunnbeløpTjeneste.mapGrunnbeløpSatser()));
    }

    private BeregningsgrunnlagInput lagBeregningsgrunnlagInput(BehandlingReferanse ref, OpptjeningAktiviteter opptjeningAktiviteter,
                                                               Collection<Inntektsmelding> inntektsmeldinger) {
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.finnGrunnlag(behandlingReferanse.getBehandlingId())).medInntektsmeldinger(inntektsmeldinger).build();
        var input = new BeregningsgrunnlagInput(MapBehandlingRef.mapRef(ref), IAYMapperTilKalkulus.mapGrunnlag(iayGrunnlag), OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(opptjeningAktiviteter), AktivitetGradering.INGEN_GRADERING, List.of(), null);
        return input;
    }

    private BehandlingReferanse lagReferanseMedStp() {
        return behandlingReferanse
            .medSkjæringstidspunkt(Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .medFørsteUttaksdato(FØRSTE_UTTAKSDAG)
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING)
            .build());
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedUbruttAktivitet() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), DAGEN_FØR_SFO, arbId1);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserGrunnbeløp(FØRSTE_UTTAKSDAG, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedAvbruttAktivitet() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(3), arbId1);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(3).plusDays(1), grunnlag);
        verifiserGrunnbeløp(FØRSTE_UTTAKSDAG.minusWeeks(3).plusDays(1), grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedLangvarigMilitærtjeneste() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(2));
        var opptj2 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(3), arbId1);

        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(3).plusDays(1), grunnlag);
        verifiserGrunnbeløp(FØRSTE_UTTAKSDAG, grunnlag);

        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedKortvarigMilitærtjeneste() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(4),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(2));
        var opptj2 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(5), arbId1);
        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(5).plusDays(1), grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedKortvarigArbeidsforhold() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(4), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(2), arbId1);
        var opptj2 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(2));
        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(2).plusDays(1), grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForMilitærMedAndreAktiviteterIOpptjeningsperioden() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), arbId1);
        var opptj2 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), DAGEN_FØR_SFO);
        var opptj3 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2),
            SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1).plusDays(1), RelatertYtelseType.ARBEIDSAVKLARINGSPENGER, null);
        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2, opptj3));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1).plusDays(2), grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
    }

    @Test
    public void testSkjæringstidspunktForMilitærUtenAndreAktiviteter() {
        // Arrange
        var opptj1 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
            SKJÆRINGSTIDSPUNKT_OPPTJENING);

        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.MILITÆR_ELLER_SIVIL);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.MILITÆR_ELLER_SIVIL);
    }

    @Test
    public void testSkjæringstidspunktForKombinertArbeidstakerOgFrilanser() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), DAGEN_FØR_SFO, arbId1);
        var opptj2 = lagFrilansOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4), DAGEN_FØR_SFO);
        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.KOMBINERT_AT_FL);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.FRILANSER);
    }

    @Test
    public void testSkjæringstidspunktForFlereFrilansaktiviteter() {
        // Arrange
        var opptj1 = lagFrilansOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4), DAGEN_FØR_SFO);
        var opptj2 = lagFrilansOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(6), DAGEN_FØR_SFO);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.FRILANSER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.FRILANSER);
    }

    @Test
    public void testSkjæringstidspunktForFlereArbeidsforholdIUlikeVirksomheter() {
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4), DAGEN_FØR_SFO, arbId1);
        var opptj2 = lagArbeidOgOpptjening(ORG_NUMMER2, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(6), DAGEN_FØR_SFO, arbId2);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForFlereArbeidsforholdISammeVirksomhet() {
        // Arrange
        String orgnr = ORG_NUMMER_MED_FLERE_ARBEIDSFORHOLD;
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        var arbId3 = InternArbeidsforholdRef.nyRef();

        no.nav.abakus.iaygrunnlag.Periode periode = new no.nav.abakus.iaygrunnlag.Periode(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4), DAGEN_FØR_SFO);
        var opptjeningAktiviteter = new OpptjeningAktiviteter(
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, periode, orgnr, arbId1),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, periode, orgnr, arbId2),
            OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, periode, orgnr, arbId3));

        var inntektsmeldinger = opprettInntektsmelding(Arbeidsgiver.virksomhet(orgnr), arbId1, arbId2, arbId3);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(opptjeningAktiviteter, inntektsmeldinger);

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForKombinertArbeidstakerOgSelvstendig() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2), arbId1);
        var opptj2 = lagNæringOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4), DAGEN_FØR_SFO);
        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.KOMBINERT_AT_SN);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedSykepengerOgArbeidsforhold() {
        // Arrange
        var ytelseStørrelse1 = lagYtelseStørrelse(ORG_NUMMER);
        var ytelseStørrelse2 = YtelseStørrelseBuilder.ny()
            .medBeløp(BigDecimal.TEN)
            .medHyppighet(InntektPeriodeType.MÅNEDLIG)
            .build();

        leggTilAktørytelse(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2),
            RelatertYtelseTilstand.LØPENDE, behandlingReferanse.getSaksnummer().getVerdi(), RelatertYtelseType.SYKEPENGER,
            Collections.singletonList(ytelseStørrelse1), Arbeidskategori.ARBEIDSTAKER, false);
        leggTilAktørytelse(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2).plusDays(1), DAGEN_FØR_SFO,
            RelatertYtelseTilstand.LØPENDE, behandlingReferanse.getSaksnummer().getVerdi(), RelatertYtelseType.SYKEPENGER,
            Collections.singletonList(ytelseStørrelse2), Arbeidskategori.ARBEIDSTAKER, false);

        var opptj1 = OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID,
            new no.nav.abakus.iaygrunnlag.Periode(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(5), null), ORG_NUMMER2);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForDagpengemottakerMedSykepenger() {
        // Arrange
        var opptj1 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2),
            RelatertYtelseType.DAGPENGER, null);
        var opptj2 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO, RelatertYtelseType.SYKEPENGER, ORG_NUMMER);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.DAGPENGER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.DAGPENGER);
    }

    @Test
    public void testSkjæringstidspunktForAAPmottakerMedSykepenger() {
        // Arrange
        var opptj1 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2),
            RelatertYtelseType.ARBEIDSAVKLARINGSPENGER, null);
        var opptj2 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO, RelatertYtelseType.SYKEPENGER, ORG_NUMMER);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserGrunnbeløp(FØRSTE_UTTAKSDAG, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
    }

    @Test
    public void testPermisjonPåSkjæringstidspunktOpptjening() {
        // Assert
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);
        InternArbeidsforholdRef arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        YrkesaktivitetBuilder yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(arbeidsforholdRef);

        LocalDate permisjonFom = SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1);
        LocalDate permisjonTom = SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(1);
        Permisjon permisjon = yrkesaktivitetBuilder.getPermisjonBuilder()
            .medPeriode(permisjonFom, permisjonTom)
            .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
            .medProsentsats(BigDecimal.valueOf(100))
            .build();
        yrkesaktivitetBuilder.leggTilPermisjon(permisjon);

        Periode opptjeningPeriode = new Periode(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), permisjonFom.minusDays(1));
        var opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID, opptjeningPeriode, ORG_NUMMER);

        // Act
        BehandlingReferanse ref = lagReferanseMedStp();
        var input = lagBeregningsgrunnlagInput(ref, opptjeningAktiviteter, List.of());
        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = mapSaksbehandletAktivitet(fastsettBeregningAktiviteter.fastsettAktiviteter(input));

        BeregningsgrunnlagEntitet BeregningsgrunnlagEntitet = mapBeregningsgrunnlag(fastsettSkjæringstidspunktOgStatuser.fastsett(input,
            mapSaksbehandletAktivitet(beregningAktivitetAggregat), input.getIayGrunnlag(), grunnbeløpTjeneste.mapGrunnbeløpSatser()));

        // Assert
        assertThat(BeregningsgrunnlagEntitet.getSkjæringstidspunkt()).isEqualTo(permisjonFom);
        assertThat(beregningAktivitetAggregat.getBeregningAktiviteter()).hasSize(1);
        BeregningAktivitetEntitet ba = beregningAktivitetAggregat.getBeregningAktiviteter().get(0);
        assertThat(ba.getArbeidsgiver()).isEqualTo(arbeidsgiver);
        assertThat(ba.getPeriode().getFomDato()).isEqualTo(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1));
        assertThat(ba.getPeriode().getTomDato()).isEqualTo(permisjonFom.minusDays(1));
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedAlleAktiviteterUnntattTYogAAP() {
        var arbId1 = InternArbeidsforholdRef.nyRef();
        // Arrange
        var opptj0 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2), arbId1);
        var opptj1 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO, RelatertYtelseType.DAGPENGER, null);
        var opptj2 = lagFrilansOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO);
        var opptj3 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO);
        var opptj4 = lagNæringOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO);
        var opptj5 = lagAnnenAktivitetMedOpptjening(ArbeidType.VENTELØNN_VARTPENGER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO);
        var opptj6 = lagAnnenAktivitetMedOpptjening(ArbeidType.ETTERLØNN_SLUTTPAKKE, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO);
        iayTestUtil.lagreOppgittOpptjening(behandlingReferanse);

        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(opptj0, opptj1, opptj2, opptj3, opptj4, opptj5, opptj6));

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(opptjeningAktiviteter);

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.KOMBINERT_AT_FL_SN, AktivitetStatus.DAGPENGER);

        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.FRILANSER,
            AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, AktivitetStatus.DAGPENGER, AktivitetStatus.ARBEIDSTAKER,
            AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForDagpengemottakerMedSykepengerMedFørsteUttaksdagEtterGrunnbeløpEndring() {
        // Arrange
        var opptj1 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2),
            RelatertYtelseType.DAGPENGER, null);
        var opptj2 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO, RelatertYtelseType.SYKEPENGER, ORG_NUMMER);

        // Act
        BeregningsgrunnlagEntitet grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2));

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserGrunnbeløp(FØRSTE_UTTAKSDAG, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.DAGPENGER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.DAGPENGER);
    }

    private List<Inntektsmelding> opprettInntektsmelding(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef... arbIdListe) {
        List<Inntektsmelding> inntektsmeldinger = new ArrayList<>();
        for (var arbId : arbIdListe) {
            var im = InntektsmeldingBuilder.builder()
                .medArbeidsgiver(arbeidsgiver)
                .medInnsendingstidspunkt(LocalDateTime.now())
                .medArbeidsforholdId(arbId)
                .medBeløp(BigDecimal.valueOf(100000)).medJournalpostId(new JournalpostId(journalpostIdInc.incrementAndGet()))
                .medStartDatoPermisjon(LocalDate.now());

            inntektsmeldinger.add(im.build());
        }

        return inntektsmeldinger;
    }

    private void verifiserSkjæringstidspunkt(LocalDate skjæringstidspunkt, BeregningsgrunnlagEntitet grunnlag) {
        assertThat(grunnlag.getSkjæringstidspunkt()).isEqualTo(skjæringstidspunkt);
    }

    private void verifiserGrunnbeløp(LocalDate førsteUttaksdag, BeregningsgrunnlagEntitet grunnlag) {
        long gVerdi = beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, førsteUttaksdag).getVerdi();
        assertThat(grunnlag.getGrunnbeløp().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(gVerdi));
    }

    private void verifiserBeregningsgrunnlagPerioder(BeregningsgrunnlagEntitet grunnlag, AktivitetStatus... expectedArray) {
        assertThat(grunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        BeregningsgrunnlagPeriode bgPeriode = grunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<AktivitetStatus> actualList = bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .map(BeregningsgrunnlagPrStatusOgAndel::getAktivitetStatus).collect(Collectors.toList());
        assertThat(actualList).containsOnly(expectedArray);
        bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(this::erArbeidstakerEllerFrilans)
            .forEach(this::verifiserBeregningsperiode);
        assertThat(actualList).hasSameSizeAs(expectedArray);
    }

    private boolean erArbeidstakerEllerFrilans(BeregningsgrunnlagPrStatusOgAndel bgpsa) {
        return (AktivitetStatus.ARBEIDSTAKER.equals(bgpsa.getAktivitetStatus()))
            || (AktivitetStatus.FRILANSER.equals(bgpsa.getAktivitetStatus()));
    }

    private void verifiserBeregningsperiode(BeregningsgrunnlagPrStatusOgAndel bgpsa) {
        assertThat(bgpsa.getBeregningsperiodeFom()).isNotNull();
        assertThat(bgpsa.getBeregningsperiodeTom()).isNotNull();
    }

    private void verifiserAktivitetStatuser(BeregningsgrunnlagEntitet grunnlag, AktivitetStatus... expectedArray) {
        List<AktivitetStatus> actualList = grunnlag.getAktivitetStatuser().stream()
            .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus).collect(Collectors.toList());
        assertThat(actualList).containsOnly(expectedArray);
    }

    private YtelseStørrelse lagYtelseStørrelse(String orgnummer) {
        return YtelseStørrelseBuilder.ny()
            .medBeløp(BigDecimal.TEN)
            .medHyppighet(InntektPeriodeType.MÅNEDLIG)
            .medVirksomhet(orgnummer).build();
    }

    private OpptjeningAktiviteter.OpptjeningPeriode lagArbeidOgOpptjening(String orgNummer, LocalDate fom, LocalDate tom, InternArbeidsforholdRef arbId) {
        iayTestUtil.byggArbeidForBehandling(behandlingReferanse, SKJÆRINGSTIDSPUNKT_OPPTJENING,
            fom, tom, arbId, Arbeidsgiver.virksomhet(orgNummer));
        return OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, new Periode(fom, tom), orgNummer);
    }

    private OpptjeningAktiviteter.OpptjeningPeriode lagFrilansOgOpptjening(LocalDate fom, LocalDate tom) {
        iayTestUtil.byggPåOppgittOpptjeningForFL(false,
            Collections.singletonList(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode.of(fom, tom)));

        return OpptjeningAktiviteter.nyPeriode(OpptjeningAktivitetType.FRILANS, new Periode(fom, tom));
    }

    private OpptjeningAktiviteter.OpptjeningPeriode lagNæringOgOpptjening(LocalDate fom, LocalDate tom) {
        iayTestUtil.byggPåOppgittOpptjeningForSN(SKJÆRINGSTIDSPUNKT_OPPTJENING, false, VirksomhetType.ANNEN,
            Collections.singleton(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode.of(fom, tom)));
        return OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.NÆRING, new Periode(fom, tom), null);
    }

    private OpptjeningAktiviteter.OpptjeningPeriode lagAnnenAktivitetMedOpptjening(ArbeidType arbeidType, LocalDate fom, LocalDate tom) {
        iayTestUtil.byggPåOppgittOpptjeningAnnenAktivitet(arbeidType, fom, tom);
        return OpptjeningAktiviteter.nyPeriode(utledOpptjeningAktivitetType(arbeidType), new Periode(fom, tom));
    }

    private OpptjeningAktiviteter.OpptjeningPeriode lagYtelseMedOpptjening(LocalDate fom, LocalDate tom, RelatertYtelseType relatertYtelseType, String orgnr) {
        leggTilAktørytelse(behandlingReferanse, fom, tom, RelatertYtelseTilstand.LØPENDE, behandlingReferanse.getSaksnummer().getVerdi(),
            relatertYtelseType, Collections.singletonList(lagYtelseStørrelse(orgnr)),
            orgnr == null ? Arbeidskategori.ARBEIDSTAKER : Arbeidskategori.INAKTIV, true);

        return OpptjeningAktiviteter.nyPeriodeOrgnr(utledOpptjeningAktivitetType(relatertYtelseType), new Periode(fom, tom), orgnr);
    }

    private OpptjeningAktivitetType utledOpptjeningAktivitetType(ArbeidType arbeidType) {
        return OpptjeningAktivitetType.hentFraArbeidTypeRelasjoner()
            .get(arbeidType).stream()
            .findFirst()
            .orElse(OpptjeningAktivitetType.UDEFINERT);
    }

    private OpptjeningAktivitetType utledOpptjeningAktivitetType(RelatertYtelseType ytelseType) {
        return OpptjeningAktivitetType.hentFraRelatertYtelseTyper()
                .get(ytelseType).stream()
                .findFirst()
                .orElse(OpptjeningAktivitetType.UDEFINERT);
    }

    private void leggTilAktørytelse(BehandlingReferanse behandlingReferanse, LocalDate fom, LocalDate tom, // NOSONAR - brukes bare til test
                                    RelatertYtelseTilstand relatertYtelseTilstand, String saksnummer, RelatertYtelseType ytelseType,
                                    List<YtelseStørrelse> ytelseStørrelseList, Arbeidskategori arbeidskategori, boolean medYtelseAnvist) {
        if (medYtelseAnvist) {
            iayTestUtil.leggTilAktørytelse(behandlingReferanse, fom, tom, relatertYtelseTilstand, saksnummer, ytelseType, ytelseStørrelseList, arbeidskategori,
                no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode.of(fom, tom));
        } else {
            iayTestUtil.leggTilAktørytelse(behandlingReferanse, fom, tom, relatertYtelseTilstand, saksnummer, ytelseType, ytelseStørrelseList, arbeidskategori);
        }
    }
}
