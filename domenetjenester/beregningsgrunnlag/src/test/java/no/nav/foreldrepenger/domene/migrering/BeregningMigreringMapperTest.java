package no.nav.foreldrepenger.domene.migrering;

import static no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus.ARBEIDSTAKER;
import static no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus.DAGPENGER;
import static no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus.FRILANSER;
import static no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus.KOMBINERT_AT_FL;
import static no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus.KOMBINERT_AT_SN;
import static no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import no.nav.folketrygdloven.kalkulus.kodeverk.AvklaringsbehovDefinisjon;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;

import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.folketrygdloven.kalkulus.kodeverk.FaktaVurderingKilde;
import no.nav.folketrygdloven.kalkulus.kodeverk.SammenligningsgrunnlagType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.entiteter.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagPeriodeRegelType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
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

        var grDto = BeregningMigreringMapper.map(grunnlag, Map.of(), Set.of());

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

        var grDto = BeregningMigreringMapper.map(grunnlag, Map.of(), Set.of());

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

    @Test
    void skal_teste_gammelt_sammenligningsgrunnlag_for_sn_mappes_til_nytt() {
        var stp = LocalDate.now();
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.valueOf(100_000))
            .medSkjæringstidspunkt(stp)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(KOMBINERT_AT_SN).medHjemmel(Hjemmel.F_14_7_8_41))
            .build();
        var periode1 = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(stp, stp.plusMonths(2).minusDays(1))
            .medBruttoPrÅr(BigDecimal.valueOf(26_000))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medBeregnetPrÅr(BigDecimal.valueOf(12_000))
            .medPgi(BigDecimal.valueOf(150_000), List.of(BigDecimal.valueOf(34_000), BigDecimal.valueOf(90_000), BigDecimal.valueOf(60_000)))
            .medAktivitetStatus(SELVSTENDIG_NÆRINGSDRIVENDE)
            .build(periode1);
        var arbforRef = UUID.randomUUID().toString();
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(2L)
            .medBeregnetPrÅr(BigDecimal.valueOf(14_000))
            .medAktivitetStatus(ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsforholdRef(arbforRef).medArbeidsgiver(Arbeidsgiver.virksomhet("999999999")).medRefusjonskravPrÅr(BigDecimal.valueOf(14_000)))
            .build(periode1);
        var sg = Sammenligningsgrunnlag.builder()
            .medRapportertPrÅr(BigDecimal.valueOf(150_000))
            .medAvvikPromille(BigDecimal.ZERO)
            .medSammenligningsperiode(LocalDate.now().minusYears(1), LocalDate.now())
            .build(beregningsgrunnlag);
        var grunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag).build(1L, BeregningsgrunnlagTilstand.FASTSATT);

        var grDto = BeregningMigreringMapper.map(grunnlag, Map.of(), Set.of());

        assertThat(grDto).isNotNull();
        assertThat(grDto.getBeregningsgrunnlagTilstand()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand.FASTSATT);

        var faktaAggregat = grDto.getFaktaAggregat();
        assertThat(faktaAggregat).isNull();

        var bgDto = grDto.getBeregningsgrunnlag();
        assertThat(bgDto.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(bgDto.getSkjæringstidspunkt()).isEqualTo(LocalDate.now());
        assertThat(bgDto.getGrunnbeløp().verdi()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        assertThat(bgDto.getAktivitetStatuser()).hasSize(1);
        assertThat(bgDto.getAktivitetStatuser().getFirst().getAktivitetStatus()).isEqualTo(AktivitetStatus.KOMBINERT_AT_SN);
        assertThat(bgDto.getAktivitetStatuser().getFirst().getHjemmel()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.Hjemmel.F_14_7_8_41);

        var bgPeriodeDto = bgDto.getBeregningsgrunnlagPerioder().getFirst();

        assertThat(bgPeriodeDto.getBruttoPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(26_000));
        assertThat(bgPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);

        var snAndel = bgPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)).findFirst().orElseThrow();
        assertThat(snAndel.getAktivitetStatus()).isEqualTo(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(snAndel.getAndelsnr()).isEqualTo(1L);
        assertThat(snAndel.getBeregnetPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(snAndel.getBruttoPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(snAndel.getPgiSnitt().verdi()).isEqualByComparingTo(BigDecimal.valueOf(150_000));
        assertThat(snAndel.getPgi1().verdi()).isEqualByComparingTo(BigDecimal.valueOf(34_000));
        assertThat(snAndel.getPgi2().verdi()).isEqualByComparingTo(BigDecimal.valueOf(90_000));
        assertThat(snAndel.getPgi3().verdi()).isEqualByComparingTo(BigDecimal.valueOf(60_000));

        var atAndel = bgPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER)).findFirst().orElseThrow();
        assertThat(atAndel.getAktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
        assertThat(atAndel.getAndelsnr()).isEqualTo(2L);
        assertThat(atAndel.getBeregnetPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(14_000));
        assertThat(atAndel.getBruttoPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(14_000));
        var arbfor = atAndel.getBgAndelArbeidsforhold();
        assertThat(arbfor).isNotNull();
        assertThat(arbfor.getArbeidsgiver().getArbeidsgiverOrgnr()).isEqualTo("999999999");
        assertThat(arbfor.getArbeidsforholdRef().getAbakusReferanse()).isEqualTo(arbforRef);
        assertThat(arbfor.getRefusjonskravPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(14_000));

        // Forventer at gammel sammenligningsgrunnlag mappet til nytt
        assertThat(bgDto.getSammenligningsgrunnlagPrStatusListe()).hasSize(1);
        var sgDto = bgDto.getSammenligningsgrunnlagPrStatusListe().getFirst();
        assertThat(sgDto.getSammenligningsgrunnlagType()).isEqualTo(SammenligningsgrunnlagType.SAMMENLIGNING_SN);
        assertThat(sgDto.getSammenligningsperiode().getFom()).isEqualTo(sg.getSammenligningsperiode().getFomDato());
        assertThat(sgDto.getSammenligningsperiode().getTom()).isEqualTo(sg.getSammenligningsperiode().getTomDato());
        assertThat(sgDto.getRapportertPrÅr().verdi()).isEqualByComparingTo(sg.getRapportertPrÅr());
    }

    @Test
    void skal_teste_gammelt_sammenligningsgrunnlag_for_at_fl_mappes_til_nytt() {
        var stp = LocalDate.now();
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.valueOf(100_000))
            .medSkjæringstidspunkt(stp)
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(KOMBINERT_AT_FL).medHjemmel(Hjemmel.F_14_7_8_40))
            .build();
        var periode1 = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(stp, stp.plusMonths(2).minusDays(1))
            .medBruttoPrÅr(BigDecimal.valueOf(26_000))
            .build(beregningsgrunnlag);
        var flAndel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medBeregnetPrÅr(BigDecimal.valueOf(12_000))
            .medAktivitetStatus(FRILANSER)
            .medAvkortetPrÅr(BigDecimal.valueOf(12_000))
            .medRedusertPrÅr(BigDecimal.valueOf(12_000))
            .medAvkortetBrukersAndelPrÅr(BigDecimal.valueOf(12_000))
            .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(12_000))
            .build(periode1);
        var arbforRef = UUID.randomUUID().toString();
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(2L)
            .medBeregnetPrÅr(BigDecimal.valueOf(14_000))
            .medAktivitetStatus(ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                .medArbeidsforholdRef(arbforRef)
                .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
                .medRefusjonskravPrÅr(BigDecimal.valueOf(14_000)))
            .build(periode1);
        var sg = Sammenligningsgrunnlag.builder()
            .medRapportertPrÅr(BigDecimal.valueOf(150_000))
            .medAvvikPromille(BigDecimal.ZERO)
            .medSammenligningsperiode(LocalDate.now().minusYears(1), LocalDate.now())
            .build(beregningsgrunnlag);
        var grunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag).build(1L, BeregningsgrunnlagTilstand.FASTSATT);

        var grDto = BeregningMigreringMapper.map(grunnlag, Map.of(), Set.of());

        assertThat(grDto).isNotNull();
        assertThat(grDto.getBeregningsgrunnlagTilstand()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand.FASTSATT);

        var faktaAggregat = grDto.getFaktaAggregat();
        assertThat(faktaAggregat).isNull();

        var bgDto = grDto.getBeregningsgrunnlag();
        assertThat(bgDto.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(bgDto.getSkjæringstidspunkt()).isEqualTo(LocalDate.now());
        assertThat(bgDto.getGrunnbeløp().verdi()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        assertThat(bgDto.getAktivitetStatuser()).hasSize(1);
        assertThat(bgDto.getAktivitetStatuser().getFirst().getAktivitetStatus()).isEqualTo(AktivitetStatus.KOMBINERT_AT_FL);
        assertThat(bgDto.getAktivitetStatuser().getFirst().getHjemmel()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.Hjemmel.F_14_7_8_40);

        var bgPeriodeDto = bgDto.getBeregningsgrunnlagPerioder().getFirst();

        assertThat(bgPeriodeDto.getBruttoPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(26_000));
        assertThat(bgPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);

        var flAndelDto = bgPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.FRILANSER)).findFirst().orElseThrow();
        assertThat(flAndelDto.getAktivitetStatus()).isEqualTo(AktivitetStatus.FRILANSER);
        assertThat(flAndelDto.getAndelsnr()).isEqualTo(1L);
        assertThat(flAndelDto.getBeregnetPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(flAndelDto.getBruttoPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(flAndelDto.getAvkortetPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(flAndelDto.getAvkortetBrukersAndelPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(flAndelDto.getRedusertPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(flAndelDto.getRedusertBrukersAndelPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(flAndelDto.getDagsatsBruker()).isEqualByComparingTo(flAndel.getDagsatsBruker());


        var atAndelDto = bgPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER)).findFirst().orElseThrow();
        assertThat(atAndelDto.getAktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
        assertThat(atAndelDto.getAndelsnr()).isEqualTo(2L);
        assertThat(atAndelDto.getBeregnetPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(14_000));
        assertThat(atAndelDto.getBruttoPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(14_000));
        var arbfor = atAndelDto.getBgAndelArbeidsforhold();
        assertThat(arbfor).isNotNull();
        assertThat(arbfor.getArbeidsgiver().getArbeidsgiverOrgnr()).isEqualTo("999999999");
        assertThat(arbfor.getArbeidsforholdRef().getAbakusReferanse()).isEqualTo(arbforRef);
        assertThat(arbfor.getRefusjonskravPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(14_000));

        // Forventer at gammel sammenligningsgrunnlag mappet til nytt
        assertThat(bgDto.getSammenligningsgrunnlagPrStatusListe()).hasSize(1);
        var sgDto = bgDto.getSammenligningsgrunnlagPrStatusListe().getFirst();
        assertThat(sgDto.getSammenligningsgrunnlagType()).isEqualTo(SammenligningsgrunnlagType.SAMMENLIGNING_AT_FL);
        assertThat(sgDto.getSammenligningsperiode().getFom()).isEqualTo(sg.getSammenligningsperiode().getFomDato());
        assertThat(sgDto.getSammenligningsperiode().getTom()).isEqualTo(sg.getSammenligningsperiode().getTomDato());
        assertThat(sgDto.getRapportertPrÅr().verdi()).isEqualByComparingTo(sg.getRapportertPrÅr());
    }

    @Test
    void skal_mappe_manuell_besteberegning() {
        var stp = LocalDate.now();
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.valueOf(100_000))
            .medSkjæringstidspunkt(stp)
            .leggTilFaktaOmBeregningTilfeller(List.of(FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING, FaktaOmBeregningTilfelle.FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE))
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(ARBEIDSTAKER).medHjemmel(Hjemmel.F_14_7_8_30))
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(DAGPENGER).medHjemmel(Hjemmel.F_14_7_8_47))
            .build();
        var periode1 = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(stp, stp.plusMonths(2).minusDays(1))
            .medBruttoPrÅr(BigDecimal.valueOf(26_000))
            .build(beregningsgrunnlag);
        var andel1 = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medBeregnetPrÅr(BigDecimal.valueOf(12_000))
            .medAktivitetStatus(DAGPENGER)
            .medBesteberegningPrÅr(BigDecimal.valueOf(12_000))
            .build(periode1);
        andel1.setOpprettetTidspunkt(LocalDateTime.now());
        var arbforRef = UUID.randomUUID().toString();
        var andel2 = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(2L)
            .medBeregnetPrÅr(BigDecimal.valueOf(14_000))
            .medBesteberegningPrÅr(BigDecimal.valueOf(14_000))
            .medAktivitetStatus(ARBEIDSTAKER)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                .medArbeidsforholdRef(arbforRef)
                .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
                .medRefusjonskravPrÅr(BigDecimal.valueOf(14_000)))
            .build(periode1);
        andel2.setOpprettetTidspunkt(LocalDateTime.now());
        var grunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag).build(1L, BeregningsgrunnlagTilstand.FASTSATT);

        var grDto = BeregningMigreringMapper.map(grunnlag, Map.of(), Set.of());

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
        assertThat(faktaAktør.getSkalBesteberegnes()).isNotNull();
        assertThat(faktaAktør.getSkalBesteberegnes().getKilde()).isEqualTo(FaktaVurderingKilde.SAKSBEHANDLER);
        assertThat(faktaAktør.getSkalBesteberegnes().getVurdering()).isTrue();
        assertThat(faktaAktør.getSkalBeregnesSomMilitær()).isNull();
        assertThat(faktaAktør.getHarFLMottattYtelse()).isNull();
        assertThat(faktaAktør.getHarFLMottattYtelse()).isNull();

        var bgDto = grDto.getBeregningsgrunnlag();
        assertThat(bgDto.getBesteberegninggrunnlag()).isNull();
        assertThat(bgDto.getBeregningsgrunnlagPerioder()).hasSize(1);
        assertThat(bgDto.getSkjæringstidspunkt()).isEqualTo(LocalDate.now());
        assertThat(bgDto.getGrunnbeløp().verdi()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        assertThat(bgDto.getAktivitetStatuser()).hasSize(2);
        assertThat(bgDto.getAktivitetStatuser().stream().anyMatch(a -> a.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER) && a.getHjemmel().equals(no.nav.folketrygdloven.kalkulus.kodeverk.Hjemmel.F_14_7_8_30))).isTrue();
        assertThat(bgDto.getAktivitetStatuser().stream().anyMatch(a -> a.getAktivitetStatus().equals(AktivitetStatus.DAGPENGER) && a.getHjemmel().equals(no.nav.folketrygdloven.kalkulus.kodeverk.Hjemmel.F_14_7_8_47))).isTrue();

        var bgPeriodeDto = bgDto.getBeregningsgrunnlagPerioder().getFirst();

        assertThat(bgPeriodeDto.getBruttoPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(26_000));
        assertThat(bgPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList()).hasSize(2);

        var dpAndelDto = bgPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.DAGPENGER)).findFirst().orElseThrow();
        assertThat(dpAndelDto.getAktivitetStatus()).isEqualTo(AktivitetStatus.DAGPENGER);
        assertThat(dpAndelDto.getAndelsnr()).isEqualTo(1L);
        assertThat(dpAndelDto.getBeregnetPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(dpAndelDto.getBesteberegningPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(dpAndelDto.getBruttoPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(12_000));


        var atAndelDto = bgPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER)).findFirst().orElseThrow();
        assertThat(atAndelDto.getAktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
        assertThat(atAndelDto.getAndelsnr()).isEqualTo(2L);
        assertThat(atAndelDto.getBesteberegningPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(14_000));
        assertThat(atAndelDto.getBeregnetPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(14_000));
        assertThat(atAndelDto.getBruttoPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(14_000));
        var arbfor = atAndelDto.getBgAndelArbeidsforhold();
        assertThat(arbfor).isNotNull();
        assertThat(arbfor.getArbeidsgiver().getArbeidsgiverOrgnr()).isEqualTo("999999999");
        assertThat(arbfor.getArbeidsforholdRef().getAbakusReferanse()).isEqualTo(arbforRef);
        assertThat(arbfor.getRefusjonskravPrÅr().verdi()).isEqualByComparingTo(BigDecimal.valueOf(14_000));
    }

    @Test
    void skal_teste_mapping_av_regelsporing() {
        var stp = LocalDate.now();
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.valueOf(100_000))
            .medSkjæringstidspunkt(stp)
            .medRegelSporing("Grunnlag-input", "Grunnlag-evaluering", BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT, "1.0")
            .medRegelSporing("Grunnlag-input", "Grunnlag-evaluering", BeregningsgrunnlagRegelType.BRUKERS_STATUS, "1.0")
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(SELVSTENDIG_NÆRINGSDRIVENDE).medHjemmel(Hjemmel.F_14_7_8_35))
            .build();
        var periode1 = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(stp, stp.plusMonths(2).minusDays(1))
            .medRegelEvaluering("Periode1-input1", "Periode1-evaluering1", BeregningsgrunnlagPeriodeRegelType.FORESLÅ, "1.0")
            .medRegelEvaluering("Periode1-input2", "Periode1-evaluering2", BeregningsgrunnlagPeriodeRegelType.FORESLÅ_2, "1.0")
            .medBruttoPrÅr(BigDecimal.valueOf(12_000))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(stp.plusMonths(2), stp.plusMonths(3))
            .medRegelEvaluering("Periode2-input", "Periode2-evaluering", BeregningsgrunnlagPeriodeRegelType.FORESLÅ, "1.0")
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
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var ap = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling,
            AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS);
        var grDto = BeregningMigreringMapper.map(grunnlag, beregningsgrunnlag.getRegelSporinger(), Set.of(ap));

        assertThat(grDto).isNotNull();
        assertThat(grDto.getBeregningsgrunnlagTilstand()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand.FASTSATT);
        var grunnlagSporinger = grDto.getGrunnlagsporinger();
        assertThat(grunnlagSporinger).hasSize(2);
        var stpRegelSporing = grunnlagSporinger.stream()
            .filter(s -> s.getRegelType().equals(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT))
            .findFirst()
            .orElse(null);
        assertThat(stpRegelSporing).isNotNull();
        assertThat(stpRegelSporing.getRegelEvaluering()).isEqualTo("Grunnlag-evaluering");
        assertThat(stpRegelSporing.getRegelInput()).isEqualTo("Grunnlag-input");
        assertThat(stpRegelSporing.getRegelVersjon()).isEqualTo("1.0");
        var statusSporing = grunnlagSporinger.stream()
            .filter(s -> s.getRegelType().equals(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagRegelType.BRUKERS_STATUS))
            .findFirst()
            .orElse(null);
        assertThat(statusSporing).isNotNull();
        assertThat(statusSporing.getRegelEvaluering()).isEqualTo("Grunnlag-evaluering");
        assertThat(statusSporing.getRegelInput()).isEqualTo("Grunnlag-input");
        assertThat(statusSporing.getRegelVersjon()).isEqualTo("1.0");

        assertThat(grDto.getPeriodesporinger()).hasSize(3);
        var sporingP1Fors = grDto.getPeriodesporinger()
            .stream()
            .filter(
                p -> p.getRegelType().equals(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagPeriodeRegelType.FORESLÅ) && p.getPeriode()
                    .getFom()
                    .equals(stp))
            .findFirst()
            .orElse(null);
        assertThat(sporingP1Fors).isNotNull();
        assertThat(sporingP1Fors.getRegelEvaluering()).isEqualTo("Periode1-evaluering1");
        assertThat(sporingP1Fors.getRegelInput()).isEqualTo("Periode1-input1");
        assertThat(sporingP1Fors.getRegelVersjon()).isEqualTo("1.0");

        var sporingP1Fors2 = grDto.getPeriodesporinger()
            .stream()
            .filter(
                p -> p.getRegelType().equals(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagPeriodeRegelType.FORESLÅ_2)
                    && p.getPeriode().getFom().equals(stp))
            .findFirst()
            .orElse(null);
        assertThat(sporingP1Fors2).isNotNull();
        assertThat(sporingP1Fors2.getRegelEvaluering()).isEqualTo("Periode1-evaluering2");
        assertThat(sporingP1Fors2.getRegelInput()).isEqualTo("Periode1-input2");
        assertThat(sporingP1Fors2.getRegelVersjon()).isEqualTo("1.0");

        var sporingP2Fors = grDto.getPeriodesporinger()
            .stream()
            .filter(
                p -> p.getRegelType().equals(no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagPeriodeRegelType.FORESLÅ)
                    && p.getPeriode().getFom().equals(stp.plusMonths(2)))
            .findFirst()
            .orElse(null);
        assertThat(sporingP2Fors).isNotNull();
        assertThat(sporingP2Fors.getRegelEvaluering()).isEqualTo("Periode2-evaluering");
        assertThat(sporingP2Fors.getRegelInput()).isEqualTo("Periode2-input");
        assertThat(sporingP2Fors.getRegelVersjon()).isEqualTo("1.0");

        assertThat(grDto.getAvklaringsbehov()).hasSize(1);
        assertThat(grDto.getAvklaringsbehov().getFirst().getDefinisjon()).isEqualTo(AvklaringsbehovDefinisjon.FASTSETT_BG_AT_FL);
    }

}
