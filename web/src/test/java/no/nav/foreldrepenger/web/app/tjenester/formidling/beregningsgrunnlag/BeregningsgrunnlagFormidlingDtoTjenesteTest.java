package no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriodeRegelType;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.Hjemmel;
import no.nav.foreldrepenger.domene.modell.PeriodeÅrsak;
import no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.dto.BeregningsgrunnlagDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BeregningsgrunnlagFormidlingDtoTjenesteTest {


    @Test
    public void tester_mapping() {
        // Arrange
        BeregningsgrunnlagGrunnlagEntitet gr = BeregningsgrunnlagGrunnlagBuilder.nytt()
            .medBeregningsgrunnlag(buildBeregningsgrunnlag())
            .build(123L, BeregningsgrunnlagTilstand.FASTSATT);

        // Act
        BeregningsgrunnlagDto dto = new BeregningsgrunnlagFormidlingDtoTjeneste(gr).map().orElse(null);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getHjemmel()).isEqualTo(Hjemmel.F_14_7_8_28_8_30);
        assertThat(dto.isErBesteberegnet()).isFalse();
        assertThat(dto.getGrunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(91425));
        assertThat(dto.getAktivitetstatusListe()).hasSize(1);
        assertThat(dto.getBeregningsgrunnlagperioder()).hasSize(1);

        BeregningsgrunnlagPeriode førstePeriode = gr.getBeregningsgrunnlag().get().getBeregningsgrunnlagPerioder().get(0);
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBeregningsgrunnlagperiodeFom()).isEqualTo(førstePeriode.getBeregningsgrunnlagPeriodeFom());
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBeregningsgrunnlagperiodeTom()).isEqualTo(førstePeriode.getBeregningsgrunnlagPeriodeTom());
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getAvkortetPrÅr()).isEqualByComparingTo(førstePeriode.getAvkortetPrÅr());
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getDagsats()).isEqualTo(førstePeriode.getDagsats());
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBruttoPrÅr()).isEqualByComparingTo(førstePeriode.getBruttoPrÅr());

        BeregningsgrunnlagPrStatusOgAndel andel = førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBeregningsgrunnlagandeler()).hasSize(1);
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBeregningsgrunnlagandeler().get(0).getBeregningsperiodeFom()).isEqualTo(andel.getBeregningsperiodeFom());
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBeregningsgrunnlagandeler().get(0).getBeregningsperiodeTom()).isEqualTo(andel.getBeregningsperiodeTom());
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBeregningsgrunnlagandeler().get(0).getDagsats()).isEqualTo(andel.getDagsats());
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBeregningsgrunnlagandeler().get(0).getAktivitetStatus()).isEqualTo(andel.getAktivitetStatus());
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBeregningsgrunnlagandeler().get(0).getBruttoPrÅr()).isEqualByComparingTo(andel.getBruttoPrÅr());
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBeregningsgrunnlagandeler().get(0).getAvkortetPrÅr()).isEqualByComparingTo(andel.getAvkortetPrÅr());
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBeregningsgrunnlagandeler().get(0).getArbeidsforholdType()).isEqualTo(andel.getArbeidsforholdType());
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBeregningsgrunnlagandeler().get(0).getErNyIArbeidslivet()).isEqualTo(andel.getNyIArbeidslivet());
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBeregningsgrunnlagandeler().get(0).getArbeidsforhold().getArbeidsforholdRef()).isEqualTo(andel.getBgAndelArbeidsforhold().get().getArbeidsforholdRef().getReferanse());
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBeregningsgrunnlagandeler().get(0).getArbeidsforhold().getArbeidsgiverIdent()).isEqualTo(andel.getBgAndelArbeidsforhold().get().getArbeidsforholdOrgnr());
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBeregningsgrunnlagandeler().get(0).getArbeidsforhold().getNaturalytelseBortfaltPrÅr()).isEqualTo(andel.getBgAndelArbeidsforhold().get().getNaturalytelseBortfaltPrÅr().orElse(BigDecimal.ZERO));
        assertThat(dto.getBeregningsgrunnlagperioder().get(0).getBeregningsgrunnlagandeler().get(0).getArbeidsforhold().getNaturalytelseTilkommetPrÅr()).isEqualTo(andel.getBgAndelArbeidsforhold().get().getNaturalytelseTilkommetPrÅr().orElse(BigDecimal.ZERO));
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

    private BeregningsgrunnlagPrStatusOgAndel buildBgPrStatusOgAndel(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        BGAndelArbeidsforhold.Builder bga = BGAndelArbeidsforhold
            .builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
            .medNaturalytelseBortfaltPrÅr(BigDecimal.valueOf(3232.32))
            .medNaturalytelseTilkommetPrÅr(BigDecimal.valueOf(3234532.32))
            .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
            .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
        return BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBeregningsperiode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5))
            .medOverstyrtPrÅr(BigDecimal.valueOf(4444432.32))
            .medAvkortetPrÅr(BigDecimal.valueOf(423.23))
            .medRedusertPrÅr(BigDecimal.valueOf(52335))
            .build(beregningsgrunnlagPeriode);
    }

    private BeregningsgrunnlagEntitet buildBeregningsgrunnlag() {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(LocalDate.now())
            .medGrunnbeløp(BigDecimal.valueOf(91425))
            .medRegelloggSkjæringstidspunkt("input1", "clob1")
            .medRegelloggBrukersStatus("input2", "clob2")
            .medRegelinputPeriodisering("input3")
            .build();
        buildBgAktivitetStatus(beregningsgrunnlag);
        BeregningsgrunnlagPeriode bgPeriode = buildBeregningsgrunnlagPeriode(beregningsgrunnlag);
        buildBgPrStatusOgAndel(bgPeriode);
        return beregningsgrunnlag;
    }

    private BeregningsgrunnlagAktivitetStatus buildBgAktivitetStatus(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medHjemmel(Hjemmel.F_14_7_8_28_8_30)
            .build(beregningsgrunnlag);
    }

}
