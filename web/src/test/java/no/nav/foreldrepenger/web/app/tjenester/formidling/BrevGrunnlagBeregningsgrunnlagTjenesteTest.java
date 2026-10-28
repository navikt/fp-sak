package no.nav.foreldrepenger.web.app.tjenester.formidling;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.BesteberegningGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;
import no.nav.foreldrepenger.domene.modell.kodeverk.PeriodeÅrsak;

class BrevGrunnlagBeregningsgrunnlagTjenesteTest {
    private static final BigDecimal NAT_BORTFALT = BigDecimal.valueOf(3232);
    private static final BigDecimal NAT_TILKOMMET = BigDecimal.valueOf(2120);
    private static final BigDecimal BRUTTO = BigDecimal.valueOf(444432);



    @Test
    void tester_mapping() {
        // Arrange
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt()
            .medBeregningsgrunnlag(buildBeregningsgrunnlag())
            .build(BeregningsgrunnlagTilstand.FASTSATT);

        // Act
        var dto = new BrevGrunnlagBeregningsgrunnlagTjeneste(gr).map().orElse(null);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.hjemmel().name()).isEqualTo(Hjemmel.F_14_7_8_28_8_30.getKode());
        assertThat(dto.erBesteberegnet()).isTrue();
        assertThat(dto.seksAvDeTiBeste()).isFalse();
        assertThat(dto.grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(100000));
        assertThat(dto.aktivitetstatusListe()).hasSize(1);
        assertThat(dto.beregningsgrunnlagperioder()).hasSize(1);

        var førstePeriode = gr.getBeregningsgrunnlag().get().getBeregningsgrunnlagPerioder().get(0);
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagperiodeFom()).isEqualTo(førstePeriode.getBeregningsgrunnlagPeriodeFom());
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagperiodeTom()).isEqualTo(førstePeriode.getBeregningsgrunnlagPeriodeTom());
        assertThat(dto.beregningsgrunnlagperioder().get(0).avkortetPrÅr()).isEqualByComparingTo(BRUTTO.add(NAT_BORTFALT).subtract(NAT_TILKOMMET));
        assertThat(dto.beregningsgrunnlagperioder().get(0).dagsats()).isEqualTo(førstePeriode.getDagsats());
        assertThat(dto.beregningsgrunnlagperioder().get(0).bruttoPrÅr()).isEqualByComparingTo(førstePeriode.getBruttoPrÅr());

        var andel = førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList().get(0);
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler()).hasSize(1);
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).beregningsperiodeFom()).isEqualTo(andel.getBeregningsperiodeFom());
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).beregningsperiodeTom()).isEqualTo(andel.getBeregningsperiodeTom());
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).dagsats()).isEqualTo(andel.getDagsats());
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).aktivitetStatus().name()).isEqualTo(andel.getAktivitetStatus().name());
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).bruttoPrÅr()).isEqualByComparingTo(andel.getBruttoPrÅr());
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).avkortetPrÅr()).isEqualByComparingTo(andel.getAvkortetPrÅr());
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).arbeidsforholdType().name()).isEqualTo(andel.getArbeidsforholdType().name());
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).erNyIArbeidslivet()).isNull();
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).arbeidsforhold().arbeidsforholdRef()).isEqualTo(andel.getBgAndelArbeidsforhold().get().getArbeidsforholdRef().getReferanse());
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).arbeidsforhold().arbeidsgiverIdent()).isEqualTo(andel.getBgAndelArbeidsforhold().get().getArbeidsforholdOrgnr());
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).arbeidsforhold().naturalytelseBortfaltPrÅr()).isEqualTo(andel.getBgAndelArbeidsforhold().get().getNaturalytelseBortfaltPrÅr().orElse(BigDecimal.ZERO));
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).arbeidsforhold().naturalytelseTilkommetPrÅr()).isEqualTo(andel.getBgAndelArbeidsforhold().get().getNaturalytelseTilkommetPrÅr().orElse(BigDecimal.ZERO));
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).erTilkommetAndel()).isFalse();
    }

    private BeregningsgrunnlagPeriode buildBeregningsgrunnlagPeriode() {
        return BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(15))
            .medBruttoPrÅr(BigDecimal.valueOf(500000))
            .medAvkortetPrÅr(BigDecimal.valueOf(500000))
            .medRedusertPrÅr(BigDecimal.valueOf(500000))
            .leggTilPeriodeÅrsak(PeriodeÅrsak.UDEFINERT)
            .leggTilBeregningsgrunnlagPrStatusOgAndel(buildBgPrStatusOgAndel())
            .build();
    }

    private BeregningsgrunnlagPrStatusOgAndel buildBgPrStatusOgAndel() {
        var bga = BGAndelArbeidsforhold
            .builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
            .medNaturalytelseBortfaltPrÅr(NAT_BORTFALT)
            .medNaturalytelseTilkommetPrÅr(NAT_TILKOMMET)
            .medArbeidsperiodeFom(LocalDate.now().minusYears(1))
            .medArbeidsperiodeTom(LocalDate.now().plusYears(2));
        return BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(bga)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medBeregningsperiode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5))
            .medArbforholdType(OpptjeningAktivitetType.ARBEID)
            .medOverstyrtPrÅr(BRUTTO)
            .medBruttoPrÅr(BRUTTO)
            .medAvkortetPrÅr(BigDecimal.valueOf(423.23))
            .medRedusertPrÅr(BigDecimal.valueOf(52335))
            .medDagsatsArbeidsgiver(100L)
            .medDagsatsBruker(200L)
            .medKilde(AndelKilde.PROSESS_START)
            .build();
    }

    private Beregningsgrunnlag buildBeregningsgrunnlag() {
        return Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(LocalDate.now())
            .leggTilAktivitetStatus(buildBgAktivitetStatus())
            .leggTilBeregningsgrunnlagPeriode(buildBeregningsgrunnlagPeriode())
            .medGrunnbeløp(BigDecimal.valueOf(100000))
            .medBesteberegningsgrunnlag(buildBesteberegning())
            .build();
    }

    private BesteberegningGrunnlag buildBesteberegning() {
        return BesteberegningGrunnlag.ny().medAvvik(BigDecimal.TEN).build();
    }

    private BeregningsgrunnlagAktivitetStatus buildBgAktivitetStatus() {
        return BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medHjemmel(Hjemmel.F_14_7_8_28_8_30)
            .build();
    }

}
