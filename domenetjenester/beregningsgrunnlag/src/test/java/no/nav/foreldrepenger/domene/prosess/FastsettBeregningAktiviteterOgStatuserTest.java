package no.nav.foreldrepenger.domene.prosess;

import static no.nav.foreldrepenger.domene.mappers.fra_kalkulus.KalkulusTilBehandlingslagerMapper.mapBeregningsgrunnlag;
import static no.nav.foreldrepenger.domene.mappers.fra_kalkulus.KalkulusTilBehandlingslagerMapper.mapSaksbehandletAktivitet;
import static no.nav.foreldrepenger.domene.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper.mapSaksbehandletAktivitet;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.folketrygdloven.kalkulator.adapter.vltilregelmodell.MapBeregningAktiviteterFraVLTilRegelK14;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.FastsettBeregningsaktiviteterInput;
import no.nav.folketrygdloven.kalkulator.input.StegProsesseringInput;
import no.nav.folketrygdloven.kalkulator.steg.fastsettskjæringstidspunkt.FastsettBeregningAktiviteter;
import no.nav.folketrygdloven.kalkulator.steg.fastsettskjæringstidspunkt.FastsettSkjæringstidspunktOgStatuser;
import no.nav.folketrygdloven.kalkulator.steg.fastsettskjæringstidspunkt.ytelse.k14.FastsettSkjæringstidspunktOgStatuserK14;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektPeriodeType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.IAYMapperTilKalkulus;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.MapBehandlingRef;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.OpptjeningMapperTilKalkulus;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.prosess.testutilities.behandling.ScenarioForeldrepenger;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class FastsettBeregningAktiviteterOgStatuserTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OPPTJENING = LocalDate.of(2018, Month.APRIL, 10);
    private static final LocalDate FØRSTE_UTTAKSDAG = SKJÆRINGSTIDSPUNKT_OPPTJENING.plusYears(1);
    private static final LocalDate DAGEN_FØR_SFO = SKJÆRINGSTIDSPUNKT_OPPTJENING.minusDays(1);

    private static final String ORG_NUMMER = "915933149";
    private static final String ORG_NUMMER2 = "974760673";
    private static final String ORG_NUMMER_MED_FLERE_ARBEIDSFORHOLD = ORG_NUMMER;

    private InntektArbeidYtelseTjeneste iayTjeneste;

    private RepositoryProvider repositoryProvider;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private GrunnbeløpTjeneste grunnbeløpTjeneste;
    private BeregningIAYTestUtil iayTestUtil;

    private FastsettBeregningAktiviteter fastsettBeregningAktiviteter;
    private FastsettSkjæringstidspunktOgStatuser fastsettSkjæringstidspunktOgStatuser;

    private final AtomicLong journalpostIdInc = new AtomicLong(123L);

    @BeforeEach
    public void setup(EntityManager entityManager) {
        beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(entityManager);
        grunnbeløpTjeneste = new GrunnbeløpTjeneste(beregningsgrunnlagRepository);
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        iayTestUtil = new BeregningIAYTestUtil(iayTjeneste);
        fastsettBeregningAktiviteter = new FastsettBeregningAktiviteter(new UnitTestLookupInstanceImpl<>(new MapBeregningAktiviteterFraVLTilRegelK14()));
        fastsettSkjæringstidspunktOgStatuser = new FastsettSkjæringstidspunktOgStatuserK14();
        repositoryProvider = new RepositoryProvider(entityManager);
    }

    private BehandlingReferanse opprettBehandling() {
        var behandling = ScenarioForeldrepenger.nyttScenario().lagre(repositoryProvider);
        return BehandlingReferanse.fra(behandling);
    }

    private BeregningsgrunnlagEntitet act(OpptjeningAktiviteter opptjeningAktiviteter, BehandlingReferanse behandling) {
        return act(opptjeningAktiviteter, List.of(), behandling);
    }

    private BeregningsgrunnlagEntitet act(OpptjeningAktiviteter opptjeningAktiviteter,
            Collection<Inntektsmelding> inntektsmeldinger,
            BehandlingReferanse behandling) {
        var ref = lagReferanseMedStp(behandling);
        var input = lagBeregningsgrunnlagInput(ref, opptjeningAktiviteter, inntektsmeldinger);
        var beregningAktivitetAggregat = fastsettBeregningAktiviteter.fastsettAktiviteter(lagStartInput(input));
        return mapBeregningsgrunnlag(fastsettSkjæringstidspunktOgStatuser.fastsett(input, beregningAktivitetAggregat,
                grunnbeløpTjeneste.mapGrunnbeløpSatser()).getBeregningsgrunnlag(), Optional.empty(), Optional.empty());
    }

    private BeregningsgrunnlagInput lagBeregningsgrunnlagInput(BehandlingReferanse ref,
            OpptjeningAktiviteter opptjeningAktiviteter,
            Collection<Inntektsmelding> inntektsmeldinger) {
        var iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.oppdatere(iayTjeneste.finnGrunnlag(ref.getBehandlingId()))
                .medInntektsmeldinger(inntektsmeldinger).build();
        return new BeregningsgrunnlagInput(MapBehandlingRef.mapRef(ref), IAYMapperTilKalkulus.mapGrunnlag(iayGrunnlag, ref.getAktørId()),
                OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(opptjeningAktiviteter), List.of(), null);
    }

    private BehandlingReferanse lagReferanseMedStp(BehandlingReferanse behandling) {
        return behandling
                .medSkjæringstidspunkt(Skjæringstidspunkt.builder()
                        .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING)
                        .medFørsteUttaksdato(FØRSTE_UTTAKSDAG)
                        .medFørsteUttaksdatoGrunnbeløp(FØRSTE_UTTAKSDAG)
                        .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING)
                        .build());
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedUbruttAktivitet() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var behandling = opprettBehandling();
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
                DAGEN_FØR_SFO, arbId1, behandling);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1), behandling);

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
        var behandling = opprettBehandling();
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(3), arbId1, behandling);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1), behandling);

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
        var behandling = opprettBehandling();
        var opptj1 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE,
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(2));
        var opptj2 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(3), arbId1, behandling);

        iayTestUtil.lagreOppgittOpptjening(behandling);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2), behandling);

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
        var behandling = opprettBehandling();
        var opptj1 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE,
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(4), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(2));
        var opptj2 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(5), arbId1, behandling);
        iayTestUtil.lagreOppgittOpptjening(behandling);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2), behandling);

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(5).plusDays(1), grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedKortvarigArbeidsforhold() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var behandling = opprettBehandling();
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(4),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(2), arbId1, behandling);
        var opptj2 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE,
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(2));
        iayTestUtil.lagreOppgittOpptjening(behandling);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2), behandling);

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusWeeks(2).plusDays(1), grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForMilitærMedAndreAktiviteterIOpptjeningsperioden() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var behandling = opprettBehandling();
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), arbId1, behandling);
        var opptj2 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE,
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), DAGEN_FØR_SFO);
        var opptj3 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1).plusDays(1), RelatertYtelseType.ARBEIDSAVKLARINGSPENGER, null,
                behandling);
        iayTestUtil.lagreOppgittOpptjening(behandling);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2, opptj3), behandling);

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1).plusDays(2), grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
    }

    @Test
    public void testSkjæringstidspunktForMilitærUtenAndreAktiviteter() {
        // Arrange
        var opptj1 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE,
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING);

        var behandling = opprettBehandling();
        iayTestUtil.lagreOppgittOpptjening(behandling);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1), behandling);

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.MILITÆR_ELLER_SIVIL);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.MILITÆR_ELLER_SIVIL);
    }

    @Test
    public void testSkjæringstidspunktForKombinertArbeidstakerOgFrilanser() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var behandling = opprettBehandling();
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), DAGEN_FØR_SFO,
                arbId1, behandling);
        var opptj2 = lagFrilansOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4), DAGEN_FØR_SFO);
        iayTestUtil.lagreOppgittOpptjening(behandling);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2), behandling);

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
        var grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2), opprettBehandling());

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
        var behandling = opprettBehandling();
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4),
                DAGEN_FØR_SFO, arbId1, behandling);
        var opptj2 = lagArbeidOgOpptjening(ORG_NUMMER2, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(6),
                DAGEN_FØR_SFO, arbId2, behandling);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2), behandling);

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForFlereArbeidsforholdISammeVirksomhet() {
        // Arrange
        var orgnr = ORG_NUMMER_MED_FLERE_ARBEIDSFORHOLD;
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        var arbId3 = InternArbeidsforholdRef.nyRef();

        var periode = new Periode(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4),
                DAGEN_FØR_SFO);
        var ref = opprettBehandling();
        var opptjeningAktiviteter = new OpptjeningAktiviteter(lagArbeidOgOpptjening(orgnr, periode.getFom(), periode.getTom(), arbId1, ref),
        lagArbeidOgOpptjening(orgnr, periode.getFom(), periode.getTom(), arbId2, ref),
        lagArbeidOgOpptjening(orgnr, periode.getFom(), periode.getTom(), arbId3, ref));

        var inntektsmeldinger = opprettInntektsmelding(Arbeidsgiver.virksomhet(orgnr), arbId1, arbId2, arbId3);

        // Act
        var grunnlag = act(opptjeningAktiviteter, inntektsmeldinger, ref);

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.ARBEIDSTAKER, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForKombinertArbeidstakerOgSelvstendig() {
        var arbId1 = InternArbeidsforholdRef.nyRef();

        // Arrange
        var behandling = opprettBehandling();
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2), arbId1, behandling);
        var opptj2 = lagNæringOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(4), DAGEN_FØR_SFO);
        iayTestUtil.lagreOppgittOpptjening(behandling);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2), behandling);

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

        var behandling = opprettBehandling();
        var opptj1 = lagArbeidOgOpptjening(ORG_NUMMER2, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(5), null, InternArbeidsforholdRef.nullRef(), behandling);
        leggTilAktørytelse(behandling, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10), SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2),
                RelatertYtelseTilstand.LØPENDE, behandling.getSaksnummer().getVerdi(), RelatertYtelseType.SYKEPENGER,
                Collections.singletonList(ytelseStørrelse1), Arbeidskategori.ARBEIDSTAKER, false);
        leggTilAktørytelse(behandling, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2).plusDays(1), DAGEN_FØR_SFO,
                RelatertYtelseTilstand.LØPENDE, behandling.getSaksnummer().getVerdi(), RelatertYtelseType.SYKEPENGER,
                Collections.singletonList(ytelseStørrelse2), Arbeidskategori.ARBEIDSTAKER, false);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1), behandling);

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSTAKER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSTAKER);
    }

    @Test
    public void testSkjæringstidspunktForDagpengemottakerMedSykepenger() {
        // Arrange
        var behandling = opprettBehandling();
        var opptj1 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2), RelatertYtelseType.DAGPENGER, null, behandling);
        var opptj2 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2),
                DAGEN_FØR_SFO, RelatertYtelseType.SYKEPENGER, ORG_NUMMER, behandling);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2), behandling);

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.DAGPENGER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.DAGPENGER);
    }

    @Test
    public void testSkjæringstidspunktForAAPmottakerMedSykepenger() {
        // Arrange
        var behandling = opprettBehandling();
        var opptj1 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2), RelatertYtelseType.ARBEIDSAVKLARINGSPENGER, null, behandling);
        var opptj2 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2),
                DAGEN_FØR_SFO, RelatertYtelseType.SYKEPENGER, ORG_NUMMER, behandling);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2), behandling);

        // Assert
        verifiserSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT_OPPTJENING, grunnlag);
        verifiserGrunnbeløp(FØRSTE_UTTAKSDAG, grunnlag);
        verifiserAktivitetStatuser(grunnlag, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
        verifiserBeregningsgrunnlagPerioder(grunnlag, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
    }

    @Test
    public void testPermisjonPåSkjæringstidspunktOpptjening() {
        // Assert
        var arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);
        var arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(arbeidsforholdRef);

        var permisjonFom = SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(1);
        var permisjonTom = SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(1);
        var permisjon = yrkesaktivitetBuilder.getPermisjonBuilder()
                .medPeriode(permisjonFom, permisjonTom)
                .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                .medProsentsats(BigDecimal.valueOf(100))
                .build();
        yrkesaktivitetBuilder.leggTilPermisjon(permisjon);

        var opptjeningPeriode = new Periode(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1), permisjonFom.minusDays(1));
        var ref = lagReferanseMedStp(opprettBehandling());
        var opptjeningAktiviteter = new OpptjeningAktiviteter(lagArbeidOgOpptjening(ORG_NUMMER, opptjeningPeriode.getFom(),
            opptjeningPeriode.getTom(), InternArbeidsforholdRef.nullRef(), ref));

        // Act
        var input = lagBeregningsgrunnlagInput(ref, opptjeningAktiviteter, List.of());
        var beregningAktivitetAggregat = mapSaksbehandletAktivitet(
                fastsettBeregningAktiviteter.fastsettAktiviteter(lagStartInput(input)));

        var BeregningsgrunnlagEntitet = mapBeregningsgrunnlag(fastsettSkjæringstidspunktOgStatuser.fastsett(input,
                mapSaksbehandletAktivitet(beregningAktivitetAggregat), grunnbeløpTjeneste.mapGrunnbeløpSatser())
                .getBeregningsgrunnlag(), Optional.empty(), Optional.empty());

        // Assert
        assertThat(BeregningsgrunnlagEntitet.getSkjæringstidspunkt()).isEqualTo(permisjonFom);
        assertThat(beregningAktivitetAggregat.getBeregningAktiviteter()).hasSize(1);
        var ba = beregningAktivitetAggregat.getBeregningAktiviteter().get(0);
        assertThat(ba.getArbeidsgiver()).isEqualTo(arbeidsgiver);
        assertThat(ba.getPeriode().getFomDato()).isEqualTo(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusYears(1));
        assertThat(ba.getPeriode().getTomDato()).isEqualTo(permisjonFom.minusDays(1));
    }

    @Test
    public void testSkjæringstidspunktForArbeidstakerMedAlleAktiviteterUnntattTYogAAP() {
        var arbId1 = InternArbeidsforholdRef.nyRef();
        // Arrange
        var behandling = opprettBehandling();
        var opptj0 = lagArbeidOgOpptjening(ORG_NUMMER, SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2), arbId1, behandling);
        var opptj1 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2),
                DAGEN_FØR_SFO, RelatertYtelseType.DAGPENGER, null, behandling);
        var opptj2 = lagFrilansOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO);
        var opptj3 = lagAnnenAktivitetMedOpptjening(ArbeidType.MILITÆR_ELLER_SIVILTJENESTE,
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO);
        var opptj4 = lagNæringOgOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO);
        var opptj5 = lagAnnenAktivitetMedOpptjening(ArbeidType.VENTELØNN_VARTPENGER,
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO);
        var opptj6 = lagAnnenAktivitetMedOpptjening(ArbeidType.ETTERLØNN_SLUTTPAKKE,
                SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2), DAGEN_FØR_SFO);
        iayTestUtil.lagreOppgittOpptjening(behandling);

        var opptjeningAktiviteter = new OpptjeningAktiviteter(List.of(opptj0, opptj1, opptj2, opptj3, opptj4, opptj5, opptj6));

        // Act
        var grunnlag = act(opptjeningAktiviteter, behandling);

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
        var behandling = opprettBehandling();
        var opptj1 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(10),
                SKJÆRINGSTIDSPUNKT_OPPTJENING.plusMonths(2), RelatertYtelseType.DAGPENGER, null, behandling);
        var opptj2 = lagYtelseMedOpptjening(SKJÆRINGSTIDSPUNKT_OPPTJENING.minusMonths(2),
                DAGEN_FØR_SFO, RelatertYtelseType.SYKEPENGER, ORG_NUMMER, behandling);

        // Act
        var grunnlag = act(new OpptjeningAktiviteter(opptj1, opptj2), behandling);

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
        var gVerdi = beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, førsteUttaksdag).getVerdi();
        assertThat(grunnlag.getGrunnbeløp().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(gVerdi));
    }

    private void verifiserBeregningsgrunnlagPerioder(BeregningsgrunnlagEntitet grunnlag, AktivitetStatus... expectedArray) {
        assertThat(grunnlag.getBeregningsgrunnlagPerioder()).hasSize(1);
        var bgPeriode = grunnlag.getBeregningsgrunnlagPerioder().get(0);
        var actualList = bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
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
        var actualList = grunnlag.getAktivitetStatuser().stream()
                .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus).collect(Collectors.toList());
        assertThat(actualList).containsOnly(expectedArray);
    }

    private YtelseStørrelse lagYtelseStørrelse(String orgnummer) {
        return YtelseStørrelseBuilder.ny()
                .medBeløp(BigDecimal.TEN)
                .medHyppighet(InntektPeriodeType.MÅNEDLIG)
                .medVirksomhet(orgnummer).build();
    }

    private OpptjeningAktiviteter.OpptjeningPeriode lagArbeidOgOpptjening(String orgNummer,
            LocalDate fom,
            LocalDate tom,
            InternArbeidsforholdRef arbId,
            BehandlingReferanse behandling) {
        iayTestUtil.byggArbeidForBehandling(behandling, SKJÆRINGSTIDSPUNKT_OPPTJENING,
                fom, tom, arbId, Arbeidsgiver.virksomhet(orgNummer));
        return OpptjeningAktiviteter.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID, new Periode(fom, tom), orgNummer, arbId);
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

    private OpptjeningAktiviteter.OpptjeningPeriode lagYtelseMedOpptjening(LocalDate fom,
            LocalDate tom,
            RelatertYtelseType relatertYtelseType,
            String orgnr,
            BehandlingReferanse behandling) {
        leggTilAktørytelse(behandling, fom, tom, RelatertYtelseTilstand.LØPENDE, behandling.getSaksnummer().getVerdi(),
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
            iayTestUtil.leggTilAktørytelse(behandlingReferanse, fom, tom, relatertYtelseTilstand, saksnummer, ytelseType, ytelseStørrelseList,
                    arbeidskategori,
                    no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode.of(fom, tom));
        } else {
            iayTestUtil.leggTilAktørytelse(behandlingReferanse, fom, tom, relatertYtelseTilstand, saksnummer, ytelseType, ytelseStørrelseList,
                    arbeidskategori);
        }
    }

    private FastsettBeregningsaktiviteterInput lagStartInput(BeregningsgrunnlagInput input) {
        return new FastsettBeregningsaktiviteterInput(new StegProsesseringInput(input, BeregningsgrunnlagTilstand.OPPRETTET));
    }
}
