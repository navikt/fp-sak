package no.nav.foreldrepenger.domene.migrering;

import static no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus.FRILANSER;
import static no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;

import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.kalkulus.kodeverk.FaktaVurderingKilde;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;

class BeregningMigreringMapperTest {

    @Test
    void skal_teste_mapping_kun_frilans_mottar_ytelse() {
        var stp = LocalDate.now();
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.valueOf(100_000))
            .medSkjæringstidspunkt(stp)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(FRILANSER).medHjemmel(Hjemmel.F_14_7_8_38 ))
            .build();
        var periode1 = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(stp, stp.plusMonths(2).minusDays(1))
            .medBruttoPrÅr(BigDecimal.valueOf(30_000))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medAktivitetStatus(FRILANSER)
            .medBeregnetPrÅr(BigDecimal.valueOf(30_000))
            .medFastsattAvSaksbehandler(Boolean.TRUE)
            .medMottarYtelse(Boolean.TRUE, FRILANSER)
            .build(periode1);
        var grunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag).build(1L, BeregningsgrunnlagTilstand.FASTSATT);

        var grDto = BeregningMigreringMapper.map(grunnlag);

        assertThat(grDto).isNotNull();
        assertThat(grDto.getBeregningsgrunnlagTilstand()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand.FASTSATT);

        // Forventer at kun frilans mottar ytelse er satt, alt annet bør være tomt / null
        var faktaAggregat = grDto.getFaktaAggregat();
        assertThat(faktaAggregat).isNotNull();
        assertThat(faktaAggregat.getFaktaArbeidsforholdListe()).isEmpty();
        var faktaAktør = faktaAggregat.getFaktaAktør();
        assertThat(faktaAktør).isNotNull();
        assertThat(faktaAktør.getErNyIArbeidslivetSN()).isNull();
        assertThat(faktaAktør.getErNyoppstartetFL()).isNull();
        assertThat(faktaAktør.getMottarEtterlønnSluttpakke()).isNull();
        assertThat(faktaAktør.getSkalBesteberegnes()).isNull();
        assertThat(faktaAktør.getSkalBeregnesSomMilitær()).isNull();
        assertThat(faktaAktør.getHarFLMottattYtelse()).isNotNull();
        assertThat(faktaAktør.getHarFLMottattYtelse().getVurdering()).isTrue();
        assertThat(faktaAktør.getHarFLMottattYtelse().getKilde()).isEqualTo(FaktaVurderingKilde.SAKSBEHANDLER);

        var bgDto = grDto.getBeregningsgrunnlag();
        assertThat(bgDto.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(bgDto.getSkjæringstidspunkt()).isEqualTo(LocalDate.now());
        assertThat(bgDto.getGrunnbeløp().verdi()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        assertThat(bgDto.getAktivitetStatuser()).hasSize(1);
        assertThat(bgDto.getAktivitetStatuser().getFirst().getAktivitetStatus()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus.FRILANSER);
        assertThat(bgDto.getAktivitetStatuser().getFirst().getHjemmel()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.Hjemmel.F_14_7_8_38);

        var bgPeriodeDto = bgDto.getBeregningsgrunnlagPerioder().getFirst();

        assertThat(bgPeriodeDto.getBruttoPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(30_000));
        assertThat(bgPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        var andelDto = bgPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList().getFirst();
        assertThat(andelDto.getAktivitetStatus()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus.FRILANSER);
        assertThat(andelDto.getAndelsnr()).isEqualTo(1L);
        assertThat(andelDto.getBeregnetPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(30_000));
        assertThat(andelDto.getBruttoPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(30_000));
        assertThat(andelDto.getFastsattAvSaksbehandler()).isTrue();
    }


    @Test
    void skal_teste_mapping_kun_næring_ny_i_arbeidslivet() {
        var stp = LocalDate.now();
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.valueOf(100_000))
            .medSkjæringstidspunkt(stp)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(SELVSTENDIG_NÆRINGSDRIVENDE).medHjemmel(Hjemmel.F_14_7_8_35))
            .build();
        var periode1 = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(stp, stp.plusMonths(2).minusDays(1))
            .medBruttoPrÅr(BigDecimal.valueOf(12_000))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medBeregnetPrÅr(BigDecimal.valueOf(12_000))
            .medPgi(BigDecimal.valueOf(150_000), List.of(BigDecimal.valueOf(34_000), BigDecimal.valueOf(90_000), BigDecimal.valueOf(60_000)))
            .medNyIArbeidslivet(false)
            .medAktivitetStatus(SELVSTENDIG_NÆRINGSDRIVENDE)
            .build(periode1);
        var grunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag).build(1L, BeregningsgrunnlagTilstand.FASTSATT);

        var grDto = BeregningMigreringMapper.map(grunnlag);

        assertThat(grDto).isNotNull();
        assertThat(grDto.getBeregningsgrunnlagTilstand()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand.FASTSATT);

        // Forventer at kun frilans mottar ytelse er satt, alt annet bør være tomt / null
        var faktaAggregat = grDto.getFaktaAggregat();
        assertThat(faktaAggregat).isNotNull();
        assertThat(faktaAggregat.getFaktaArbeidsforholdListe()).isEmpty();
        var faktaAktør = faktaAggregat.getFaktaAktør();
        assertThat(faktaAktør).isNotNull();
        assertThat(faktaAktør.getHarFLMottattYtelse()).isNull();
        assertThat(faktaAktør.getErNyoppstartetFL()).isNull();
        assertThat(faktaAktør.getMottarEtterlønnSluttpakke()).isNull();
        assertThat(faktaAktør.getSkalBesteberegnes()).isNull();
        assertThat(faktaAktør.getSkalBeregnesSomMilitær()).isNull();
        assertThat(faktaAktør.getErNyIArbeidslivetSN()).isNotNull();
        assertThat(faktaAktør.getErNyIArbeidslivetSN().getVurdering()).isFalse();
        assertThat(faktaAktør.getErNyIArbeidslivetSN().getKilde()).isEqualTo(FaktaVurderingKilde.SAKSBEHANDLER);

        var bgDto = grDto.getBeregningsgrunnlag();
        assertThat(bgDto.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(bgDto.getSkjæringstidspunkt()).isEqualTo(LocalDate.now());
        assertThat(bgDto.getGrunnbeløp().verdi()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        assertThat(bgDto.getAktivitetStatuser()).hasSize(1);
        assertThat(bgDto.getAktivitetStatuser().getFirst().getAktivitetStatus()).isEqualTo(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(bgDto.getAktivitetStatuser().getFirst().getHjemmel()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.Hjemmel.F_14_7_8_35);

        var bgPeriodeDto = bgDto.getBeregningsgrunnlagPerioder().getFirst();

        assertThat(bgPeriodeDto.getBruttoPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(bgPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(1);
        var andelDto = bgPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList().getFirst();
        assertThat(andelDto.getAktivitetStatus()).isEqualTo(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(andelDto.getAndelsnr()).isEqualTo(1L);
        assertThat(andelDto.getBeregnetPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(andelDto.getBruttoPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(andelDto.getPgiSnitt().verdi()).isEqualByComparingTo(BigDecimal.valueOf(150_000));
        assertThat(andelDto.getPgi1().verdi()).isEqualByComparingTo(BigDecimal.valueOf(34_000));
        assertThat(andelDto.getPgi2().verdi()).isEqualByComparingTo(BigDecimal.valueOf(90_000));
        assertThat(andelDto.getPgi3().verdi()).isEqualByComparingTo(BigDecimal.valueOf(60_000));
    }
}
