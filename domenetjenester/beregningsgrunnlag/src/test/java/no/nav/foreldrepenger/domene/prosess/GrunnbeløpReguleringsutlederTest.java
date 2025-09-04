package no.nav.foreldrepenger.domene.prosess;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.vedtak.konfig.Tid;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class GrunnbeløpReguleringsutlederTest {

    @Test
    void skal_ikke_regulere_når_ikke_avkortet() {
        // Arrange
        var arbfor = BGAndelArbeidsforhold.builder().medArbeidsgiver(Arbeidsgiver.virksomhet("999999999")).build();
        var andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBruttoPrÅr(BigDecimal.valueOf(500000))
            .medAndelsnr(1L)
            .medBGAndelArbeidsforhold(arbfor)
            .build();
        var periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(LocalDate.now(), Tid.TIDENES_ENDE)
            .leggTilBeregningsgrunnlagPrStatusOgAndel(andel)
            .medBruttoPrÅr(BigDecimal.valueOf(500000))
            .build();
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(LocalDate.now())
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).build())
            .medGrunnbeløp(BigDecimal.valueOf(100000))
            .leggTilBeregningsgrunnlagPeriode(periode)
            .build();

        // Act
        var kanTrengeGregulering = GrunnbeløpReguleringsutleder.kanPåvirkesAvGrunnbeløpRegulering(bg);

        // Assert
        assertThat(kanTrengeGregulering).isFalse();
    }

    @Test
    void skal_regulere_når_avkortet() {
        // Arrange
        var arbfor = BGAndelArbeidsforhold.builder().medArbeidsgiver(Arbeidsgiver.virksomhet("999999999")).build();
        var andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBruttoPrÅr(BigDecimal.valueOf(700000))
            .medAndelsnr(1L)
            .medBGAndelArbeidsforhold(arbfor)
            .build();
        var periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(LocalDate.now(), Tid.TIDENES_ENDE)
            .leggTilBeregningsgrunnlagPrStatusOgAndel(andel)
            .medBruttoPrÅr(BigDecimal.valueOf(700000))
            .build();
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(LocalDate.now())
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER).build())
            .medGrunnbeløp(BigDecimal.valueOf(100000))
            .leggTilBeregningsgrunnlagPeriode(periode)
            .build();

        // Act
        var kanTrengeGregulering = GrunnbeløpReguleringsutleder.kanPåvirkesAvGrunnbeløpRegulering(bg);

        // Assert
        assertThat(kanTrengeGregulering).isTrue();
    }

    @Test
    void skal_regulere_når_næringsdrivendet() {
        // Arrange
        var andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medBruttoPrÅr(BigDecimal.valueOf(200000))
            .medAndelsnr(1L)
            .build();
        var periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(LocalDate.now(), Tid.TIDENES_ENDE)
            .leggTilBeregningsgrunnlagPrStatusOgAndel(andel)
            .medBruttoPrÅr(BigDecimal.valueOf(200000))
            .build();
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(LocalDate.now())
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE).build())
            .medGrunnbeløp(BigDecimal.valueOf(100000))
            .leggTilBeregningsgrunnlagPeriode(periode)
            .build();

        // Act
        var kanTrengeGregulering = GrunnbeløpReguleringsutleder.kanPåvirkesAvGrunnbeløpRegulering(bg);

        // Assert
        assertThat(kanTrengeGregulering).isTrue();
    }

    @Test
    void skal_regulere_når_militær_med_lavt_grunnlag() {
        // Arrange
        var andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.MILITÆR_ELLER_SIVIL)
            .medBruttoPrÅr(BigDecimal.valueOf(300000))
            .medAndelsnr(1L)
            .build();
        var periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(LocalDate.now(), Tid.TIDENES_ENDE)
            .leggTilBeregningsgrunnlagPrStatusOgAndel(andel)
            .medBruttoPrÅr(BigDecimal.valueOf(300000))
            .build();
        var bg = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(LocalDate.now())
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.MILITÆR_ELLER_SIVIL).build())
            .medGrunnbeløp(BigDecimal.valueOf(100000))
            .leggTilBeregningsgrunnlagPeriode(periode)
            .build();

        // Act
        var kanTrengeGregulering = GrunnbeløpReguleringsutleder.kanPåvirkesAvGrunnbeløpRegulering(bg);

        // Assert
        assertThat(kanTrengeGregulering).isTrue();
    }

}
