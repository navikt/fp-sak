package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

import no.nav.foreldrepenger.domene.prosess.PeriodeMedGradering;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;

import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static no.nav.fpsak.tidsserie.LocalDateInterval.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

class GraderingUtenBeregningsgrunnlagTjenesteTest {
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static final String ORGNR = "915933149";

    private Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

    private Beregningsgrunnlag.Builder bgBuilder = lagBeregningsgrunnlag();


    @Test
    void skalIkkeFåAvklaringsbehovArbeidstakerMedBG() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null, lagBeregningsgrunnlagAndel(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.TEN));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);
        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.emptyList());

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    void skalIkkeFåAvklaringsbehovArbeidstakerUtenGradering() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null, lagBeregningsgrunnlagAndel(AktivitetStatus.ARBEIDSTAKER, BigDecimal.ZERO));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.emptyList());

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    void skalFåAvklaringsbehovArbeidstakerNårGraderingOgHarIkkeBG() {
        // Arrange
        LocalDate skjæringstidspunkt = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingFom = skjæringstidspunkt;
        LocalDate graderingTom = skjæringstidspunkt.plusMonths(4);

        PeriodeMedGradering periodeMedGradering = new PeriodeMedGradering(graderingFom, graderingTom, BigDecimal.valueOf(50), AktivitetStatus.ARBEIDSTAKER, arbeidsgiver);

        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(skjæringstidspunkt, null, lagBeregningsgrunnlagAndel(AktivitetStatus.ARBEIDSTAKER, BigDecimal.ZERO, SKJÆRINGSTIDSPUNKT.minusYears(1), TIDENES_ENDE));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.singletonList(periodeMedGradering));

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isTrue();
    }

    @Test
    void skalFåAvklaringsbehovSelvstendigNårGraderingOgHarIkkeBG() {
        // Arrange
        LocalDate skjæringstidspunkt = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingFom = skjæringstidspunkt;
        LocalDate graderingTom = skjæringstidspunkt.plusMonths(4);


        PeriodeMedGradering periodeMedGradering = new PeriodeMedGradering(graderingFom, graderingTom, BigDecimal.valueOf(50), AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, null);

        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null, lagBeregningsgrunnlagAndel(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.ZERO));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.singletonList(periodeMedGradering));

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isTrue();
    }

    @Test
    void skalIkkeFåAvklaringsbehovSelvstendigNårGraderingOgHarBG() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null, lagBeregningsgrunnlagAndel(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.TEN));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.emptyList());

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    void skalIkkeFåAvklaringsbehovSelvstendigNårIkkeGradering() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null, lagBeregningsgrunnlagAndel(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.ZERO));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.emptyList());

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    void skalFåAvklaringsbehovFrilanserNårGraderingOgHarIkkeBG() {
        // Arrange
        LocalDate skjæringstidspunkt = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingFom = skjæringstidspunkt;
        LocalDate graderingTom = skjæringstidspunkt.plusMonths(6);

        PeriodeMedGradering periodeMedGradering = new PeriodeMedGradering(graderingFom, graderingTom, BigDecimal.valueOf(50), AktivitetStatus.FRILANSER, null);
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null, lagBeregningsgrunnlagAndel(AktivitetStatus.FRILANSER, BigDecimal.ZERO));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.singletonList(periodeMedGradering));

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isTrue();
    }

    @Test
    void skalIkkeFåAvklaringsbehovFrilanserNårGraderingOgHarBG() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null, lagBeregningsgrunnlagAndel(AktivitetStatus.FRILANSER, BigDecimal.TEN));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.emptyList());

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    void skalIkkeFåAvklaringsbehovFrilanserNårIkkeGradering() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null, lagBeregningsgrunnlagAndel(AktivitetStatus.FRILANSER, BigDecimal.ZERO));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.emptyList());

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    void skalIkkeFåAvklaringsbehovFrilanserNårGraderingUtenforPeriodeUtenBeregningsgrunnlag() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(3), lagBeregningsgrunnlagAndel(AktivitetStatus.FRILANSER, BigDecimal.ZERO));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.emptyList());

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    void skalIkkeFåAvklaringsbehovSNNårGraderingUtenforPeriodeUtenBeregningsgrunnlag() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT.plusMonths(2), null, lagBeregningsgrunnlagAndel(AktivitetStatus.FRILANSER, BigDecimal.ZERO));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.emptyList());

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    void skalIkkeFåAvklaringsbehovArbeidstakerNårGraderingUtenforPeriodeUtenBeregningsgrunnlag2() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(3), lagBeregningsgrunnlagAndel(AktivitetStatus.ARBEIDSTAKER, BigDecimal.ZERO));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.emptyList());

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    void skalIkkeFåAvklaringsbehovNårAAP() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null, lagBeregningsgrunnlagAndel(AktivitetStatus.ARBEIDSAVKLARINGSPENGER, BigDecimal.ZERO));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.emptyList());

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    @Test
    void skal_ikke_finne_andel_når_det_er_sn_med_gradering_med_inntekt_på_grunnlag() {
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null, lagBeregningsgrunnlagAndel(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.TEN));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.emptyList());
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = finnAndelerMedGraderingUtenBG(Collections.emptyList());

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
        assertThat(andeler).isEmpty();
    }

    @Test
    void skal_ikke_finne_andel_når_det_er_gradering_men_ikke_fastsatt_redusert_pr_år() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null, lagBeregningsgrunnlagAndel(AktivitetStatus.ARBEIDSTAKER, null));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.emptyList());
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = finnAndelerMedGraderingUtenBG(Collections.emptyList());

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
        assertThat(andeler).isEmpty();
    }

    @Test
    void skal_ikke_finne_andel_når_det_er_gradering_men_fastsatt_grunnlag_over_0_redusert_pr_år() {
        // Arrange
        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null, lagBeregningsgrunnlagAndel(AktivitetStatus.ARBEIDSTAKER, BigDecimal.TEN));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.emptyList());
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = finnAndelerMedGraderingUtenBG(Collections.emptyList());

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
        assertThat(andeler).isEmpty();
    }

    @Test
    void skal_finne_andel_når_det_er_gradering() {
        // Arrange
        LocalDate skjæringstidspunkt = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingFom = skjæringstidspunkt;
        LocalDate graderingTom = skjæringstidspunkt.plusMonths(4);

        PeriodeMedGradering periodeMedGradering = new PeriodeMedGradering(graderingFom, graderingTom, BigDecimal.valueOf(50), AktivitetStatus.ARBEIDSTAKER, arbeidsgiver);

        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null, lagBeregningsgrunnlagAndel(AktivitetStatus.ARBEIDSTAKER, BigDecimal.ZERO, SKJÆRINGSTIDSPUNKT.minusYears(1), TIDENES_ENDE));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.singletonList(periodeMedGradering));
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = finnAndelerMedGraderingUtenBG(Collections.singletonList(periodeMedGradering));

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isTrue();
        assertThat(andeler).hasSize(1);
    }

    @Test
    void skal_finne_andel_når_det_er_sn_med_gradering_uten_inntekt_på_grunnlag() {
        // Arrange
        LocalDate skjæringstidspunkt = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingFom = skjæringstidspunkt;
        LocalDate graderingTom = skjæringstidspunkt.plusMonths(6);

        PeriodeMedGradering periodeMedGradering = new PeriodeMedGradering(graderingFom, graderingTom, BigDecimal.valueOf(50), AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, null);

        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(skjæringstidspunkt, null, lagBeregningsgrunnlagAndel(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.ZERO));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.singletonList(periodeMedGradering));
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = finnAndelerMedGraderingUtenBG(Collections.singletonList(periodeMedGradering));

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isTrue();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
    }

    @Test
    void skal_finne_riktig_andel_når_det_er_flere_med_gradering_men_kun_en_mangler_inntekt() {
        // Arrange
        LocalDate skjæringstidspunkt = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingFom = skjæringstidspunkt;
        LocalDate graderingTom = skjæringstidspunkt.plusMonths(4);

        PeriodeMedGradering periodeMedGradering = new PeriodeMedGradering(graderingFom, graderingTom, BigDecimal.valueOf(50), AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, null);

        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null, lagBeregningsgrunnlagAndel(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.ZERO, 1L),
            lagBeregningsgrunnlagAndel(AktivitetStatus.ARBEIDSTAKER, BigDecimal.TEN, 2L));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.singletonList(periodeMedGradering));
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = finnAndelerMedGraderingUtenBG(Collections.singletonList(periodeMedGradering));

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isTrue();
        assertThat(andeler).hasSize(1);
        assertThat(andeler.get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
    }

    @Test
    void skal_gi_false_når_to_andeler_i_graderingsperiode_men_ikke_0_på_andel_som_skal_graderes() {
        // Arrange
        LocalDate skjæringstidspunkt = SKJÆRINGSTIDSPUNKT;
        LocalDate graderingFom = skjæringstidspunkt;
        LocalDate graderingTom = skjæringstidspunkt.plusMonths(4);

        PeriodeMedGradering periodeMedGradering = new PeriodeMedGradering(graderingFom, graderingTom, BigDecimal.valueOf(50), AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, null);

        var beregningsgrunnlagPeriode = lagBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, null, lagBeregningsgrunnlagAndel(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, BigDecimal.TEN, 1L),
            lagBeregningsgrunnlagAndel(AktivitetStatus.FRILANSER, BigDecimal.ZERO, 2L));
        bgBuilder.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);

        // Act
        boolean harAndelerMedGraderingUtenGrunnlag = harAndelerMedGraderingUtenGrunnlag(Collections.singletonList(periodeMedGradering));

        // Assert
        assertThat(harAndelerMedGraderingUtenGrunnlag).isFalse();
    }

    private List<BeregningsgrunnlagPrStatusOgAndel> finnAndelerMedGraderingUtenBG(List<PeriodeMedGradering> periodeMedGradering) {
        return GraderingUtenBeregningsgrunnlagTjeneste.finnesAndelerMedGraderingUtenBG(bgBuilder.build(), periodeMedGradering);
    }

    private boolean harAndelerMedGraderingUtenGrunnlag(List<PeriodeMedGradering> perioderMedGradering) {
        return !GraderingUtenBeregningsgrunnlagTjeneste.finnesAndelerMedGraderingUtenBG(bgBuilder.build(), perioderMedGradering).isEmpty();
    }

    private BeregningsgrunnlagPeriode lagBeregningsgrunnlagPeriode(LocalDate periodeFom, LocalDate periodeTom, BeregningsgrunnlagPrStatusOgAndel... andeler) {
        var andelArray = List.of(andeler);
        var periode = BeregningsgrunnlagPeriode.builder().medBeregningsgrunnlagPeriode(periodeFom, periodeTom);
        andelArray.forEach(periode::leggTilBeregningsgrunnlagPrStatusOgAndel);
        return periode.build();
    }

    private BeregningsgrunnlagPrStatusOgAndel lagBeregningsgrunnlagAndel(AktivitetStatus aktivitetStatus, BigDecimal redusertPrÅr) {
        return lagBeregningsgrunnlagAndel(aktivitetStatus, redusertPrÅr, null, null, 1L);
    }

    private BeregningsgrunnlagPrStatusOgAndel lagBeregningsgrunnlagAndel(AktivitetStatus aktivitetStatus, BigDecimal redusertPrÅr, Long andelsnr) {
        return lagBeregningsgrunnlagAndel(aktivitetStatus, redusertPrÅr, null, null, andelsnr);
    }

    private BeregningsgrunnlagPrStatusOgAndel lagBeregningsgrunnlagAndel(AktivitetStatus aktivitetStatus, BigDecimal redusertPrÅr, LocalDate arbeidsperiodeFom, LocalDate arbeidsperiodeTom) {
        return lagBeregningsgrunnlagAndel(aktivitetStatus, redusertPrÅr, arbeidsperiodeFom, arbeidsperiodeTom, 1L);
    }

    private BeregningsgrunnlagPrStatusOgAndel lagBeregningsgrunnlagAndel(AktivitetStatus aktivitetStatus, BigDecimal redusertPrÅr, LocalDate arbeidsperiodeFom, LocalDate arbeidsperiodeTom, Long andelsnr) {

        BGAndelArbeidsforhold.Builder bgAndelBuilder = BGAndelArbeidsforhold.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsperiodeFom(arbeidsperiodeFom)
            .medArbeidsperiodeTom(arbeidsperiodeTom);
        BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(aktivitetStatus)
            .medAndelsnr(andelsnr)
            .medRedusertPrÅr(redusertPrÅr);

        if (aktivitetStatus.erArbeidstaker()) {
            andelBuilder.medBGAndelArbeidsforhold(bgAndelBuilder);
        }

        return andelBuilder.build();
    }

    private Beregningsgrunnlag.Builder lagBeregningsgrunnlag() {
        return Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT);
    }
}
