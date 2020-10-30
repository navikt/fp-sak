package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.PeriodeÅrsak;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Sammenligningsgrunnlag;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class OpprettGrunnbeløpTaskTest extends EntityManagerAwareTest {

    private static final String ORGNR = "55";
    private static final BeregningsgrunnlagTilstand STEG_OPPRETTET = BeregningsgrunnlagTilstand.OPPRETTET;
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusDays(5);

    private BehandlingRepositoryProvider repositoryProvider;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    private OpprettGrunnbeløpTask opprettGrunnbeløpTask;

    private final ScenarioMorSøkerForeldrepenger scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel();

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(entityManager);
        opprettGrunnbeløpTask = new OpprettGrunnbeløpTask(beregningsgrunnlagRepository);
    }

    @Test
    public void settBeregningsgrunnlagIkkeAktiv() {
        var behandling = opprettBehandling();
        // Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = buildBeregningsgrunnlagUtenGrunnbeløp();
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, STEG_OPPRETTET);

        assertThat(beregningsgrunnlag.getGrunnbeløp()).isNull();


        BeregningsgrunnlagEntitet beregningsgrunnlag2 = buildBeregningsgrunnlag();
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag2, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);

        // Act
        ProsessTaskData taskParameter = new ProsessTaskData(OpprettGrunnbeløpTask.TASKNAME);
        taskParameter.setProperty(ProsessTaskData.BEHANDLING_ID, behandling.getId().toString());
        opprettGrunnbeløpTask.doTask(taskParameter);



        Optional<BeregningsgrunnlagGrunnlagEntitet> entitetOpt = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(behandling.getId(), STEG_OPPRETTET);
        //Assert
        assertThat(entitetOpt).as("entitetOpt").hasValueSatisfying(entitet -> {
            assertThat(entitet.erAktivt()).as("entitet.aktiv").isFalse();
            assertThat(entitet.getBeregningsgrunnlag().get().getGrunnbeløp()).isNotNull();

        });
    }

    private Behandling opprettBehandling() {
        return scenarioMorSøkerForeldrepenger.lagre(repositoryProvider);
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

    private BeregningsgrunnlagEntitet buildBeregningsgrunnlagUtenGrunnbeløp() {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
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

    private BeregningsgrunnlagPrStatusOgAndel buildBgPrStatusOgAndel(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold
            .builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
            .medNaturalytelseBortfaltPrÅr(BigDecimal.valueOf(3232.32))
            .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
            .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
        return BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga)
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medBeregningsperiode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5))
            .medOverstyrtPrÅr(BigDecimal.valueOf(4444432.32))
            .medAvkortetPrÅr(BigDecimal.valueOf(423.23))
            .medRedusertPrÅr(BigDecimal.valueOf(52335))
            .build(beregningsgrunnlagPeriode);
    }

    private BeregningsgrunnlagAktivitetStatus buildBgAktivitetStatus(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build(beregningsgrunnlag);
    }

    private Sammenligningsgrunnlag buildSammenligningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return Sammenligningsgrunnlag.builder()
            .medSammenligningsperiode(LocalDate.now().minusDays(12), LocalDate.now().minusDays(6))
            .medRapportertPrÅr(BigDecimal.valueOf(323212.12))
            .medAvvikPromille(BigDecimal.valueOf(120))
            .build(beregningsgrunnlag);
    }
}
