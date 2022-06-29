package no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagPeriodeRegelType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;
import no.nav.foreldrepenger.domene.modell.kodeverk.PeriodeÅrsak;

class BeregningsgrunnlagFormidlingV2DtoTjenesteTest {
    private static final BigDecimal NAT_BORTFALT = BigDecimal.valueOf(3232);
    private static final BigDecimal NAT_TILKOMMET = BigDecimal.valueOf(2120);
    private static final BigDecimal BRUTTO = BigDecimal.valueOf(444432);



    @Test
    public void tester_mapping() {
        // Arrange
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt()
            .medBeregningsgrunnlag(buildBeregningsgrunnlag())
            .build(123L, BeregningsgrunnlagTilstand.FASTSATT);

        // Act
        var dto = new BeregningsgrunnlagFormidlingV2DtoTjeneste(gr).map().orElse(null);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.hjemmel().name()).isEqualTo(Hjemmel.F_14_7_8_28_8_30.getKode());
        assertThat(dto.erBesteberegnet()).isFalse();
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
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).erNyIArbeidslivet()).isEqualTo(andel.getNyIArbeidslivet());
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).arbeidsforhold().arbeidsforholdRef()).isEqualTo(andel.getBgAndelArbeidsforhold().get().getArbeidsforholdRef().getReferanse());
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).arbeidsforhold().arbeidsgiverIdent()).isEqualTo(andel.getBgAndelArbeidsforhold().get().getArbeidsforholdOrgnr());
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).arbeidsforhold().naturalytelseBortfaltPrÅr()).isEqualTo(andel.getBgAndelArbeidsforhold().get().getNaturalytelseBortfaltPrÅr().orElse(BigDecimal.ZERO));
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).arbeidsforhold().naturalytelseTilkommetPrÅr()).isEqualTo(andel.getBgAndelArbeidsforhold().get().getNaturalytelseTilkommetPrÅr().orElse(BigDecimal.ZERO));
        assertThat(dto.beregningsgrunnlagperioder().get(0).beregningsgrunnlagandeler().get(0).erTilkommetAndel()).isFalse();
    }

    private BeregningsgrunnlagPeriode buildBeregningsgrunnlagPeriode(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(LocalDate.now().minusDays(20), LocalDate.now().minusDays(15))
            .medBruttoPrÅr(BigDecimal.valueOf(500000))
            .medAvkortetPrÅr(BigDecimal.valueOf(500000))
            .medRedusertPrÅr(BigDecimal.valueOf(500000))
            .medRegelEvaluering("input1", "clob1", BeregningsgrunnlagPeriodeRegelType.FORESLÅ)
            .medRegelEvaluering("input2", "clob2", BeregningsgrunnlagPeriodeRegelType.FASTSETT)
            .leggTilPeriodeÅrsak(PeriodeÅrsak.UDEFINERT)
            .build(beregningsgrunnlag);
    }

    private BeregningsgrunnlagPrStatusOgAndel buildBgPrStatusOgAndel(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
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
            .medOverstyrtPrÅr(BRUTTO)
            .medAvkortetPrÅr(BigDecimal.valueOf(423.23))
            .medRedusertPrÅr(BigDecimal.valueOf(52335))
            .medKilde(AndelKilde.PROSESS_START)
            .build(beregningsgrunnlagPeriode);
    }

    private BeregningsgrunnlagEntitet buildBeregningsgrunnlag() {
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(LocalDate.now())
            .medGrunnbeløp(BigDecimal.valueOf(100000))
            .medRegelloggSkjæringstidspunkt("input1", "clob1")
            .medRegelloggBrukersStatus("input2", "clob2")
            .medRegelinputPeriodisering("input3")
            .build();
        buildBgAktivitetStatus(beregningsgrunnlag);
        var bgPeriode = buildBeregningsgrunnlagPeriode(beregningsgrunnlag);
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
