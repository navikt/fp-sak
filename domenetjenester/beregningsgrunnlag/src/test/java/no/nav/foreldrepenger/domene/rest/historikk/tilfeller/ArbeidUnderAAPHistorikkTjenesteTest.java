package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsettInntektForArbeidUnderAAPDto;
import no.nav.foreldrepenger.domene.typer.Beløp;

class ArbeidUnderAAPHistorikkTjenesteTest {

    private static final LocalDate STP = LocalDate.now();

    private static ArbeidUnderAAPHistorikkTjeneste historikkTjeneste;
    private static BeregningsgrunnlagEntitet beregningsgrunnlag;
    private static InntektArbeidYtelseGrunnlag iayGrunnlag;

    @BeforeAll
    static void setup() {
        historikkTjeneste = new ArbeidUnderAAPHistorikkTjeneste();
        beregningsgrunnlag = BeregningsgrunnlagEntitet.ny().medGrunnbeløp(new Beløp(600000)).medSkjæringstidspunkt(STP).build();
        iayGrunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();
    }

    @Test
    void oppdaterer_fra_null_forrige_til_verdi() {
        var faktaBeregningLagreDto = lagFaktaBeregningLagreDto(100000);

        var historikkLinjeBuilder = historikkTjeneste.lagHistorikk(faktaBeregningLagreDto, beregningsgrunnlag, Optional.empty(), iayGrunnlag);
        var faktiskeLinjer = historikkLinjeBuilder.stream().map(HistorikkinnslagLinjeBuilder::tilTekst).toList();

        assertThat(faktiskeLinjer).hasSize(2);
        assertThat(faktiskeLinjer.getFirst()).isEqualTo("__Inntekten__ er satt til __100000__");
        assertThat(faktiskeLinjer.get(1)).isEmpty();
    }

    @Test
    void oppdaterer_fra_null_beregnetPrÅr_til_ny_verdi() {
        var faktaBeregningLagreDto = lagFaktaBeregningLagreDto(100000);
        var forrigeBg = lagForrigeBeregningsgrunnlag(null);

        var historikkLinjeBuilder = historikkTjeneste.lagHistorikk(faktaBeregningLagreDto, beregningsgrunnlag, forrigeBg, iayGrunnlag);
        var faktiskeLinjer = historikkLinjeBuilder.stream().map(HistorikkinnslagLinjeBuilder::tilTekst).toList();

        assertThat(faktiskeLinjer).hasSize(2);
        assertThat(faktiskeLinjer.getFirst()).isEqualTo("__Inntekten__ er satt til __100000__");
        assertThat(faktiskeLinjer.get(1)).isEmpty();
    }

    @Test
    void oppdaterer_fra_verdi_til_ny_verdi() {
        var faktaBeregningLagreDto = lagFaktaBeregningLagreDto(100000);
        var forrigeBg = lagForrigeBeregningsgrunnlag(BigDecimal.valueOf(360000));

        var historikkLinjeBuilder = historikkTjeneste.lagHistorikk(faktaBeregningLagreDto, beregningsgrunnlag, forrigeBg, iayGrunnlag);
        var faktiskeLinjer = historikkLinjeBuilder.stream().map(HistorikkinnslagLinjeBuilder::tilTekst).toList();

        assertThat(faktiskeLinjer).hasSize(2);
        assertThat(faktiskeLinjer.getFirst()).isEqualTo("__Inntekten__ er endret fra 30000 til __100000__");
        assertThat(faktiskeLinjer.get(1)).isEmpty();
    }

    @Test
    void oppdaterer_fra_verdi_til_samme_verdi_gir_ingen_innslag() {
        var faktaBeregningLagreDto = lagFaktaBeregningLagreDto(30000);
        var forrigeBg = lagForrigeBeregningsgrunnlag(BigDecimal.valueOf(360000));

        var historikkLinjeBuilder = historikkTjeneste.lagHistorikk(faktaBeregningLagreDto, beregningsgrunnlag, forrigeBg, iayGrunnlag);
        var faktiskeLinjer = historikkLinjeBuilder.stream().map(HistorikkinnslagLinjeBuilder::tilTekst).toList();

        assertThat(faktiskeLinjer).isEmpty();
    }

    private static FaktaBeregningLagreDto lagFaktaBeregningLagreDto(Integer fastsattPrMnd) {
        var faktaBeregningLagreDto = new FaktaBeregningLagreDto(
            Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_INNTEKT_FOR_ARBEID_UNDER_AAP));
        faktaBeregningLagreDto.setFastsettArbeidUnderAap(new FastsettInntektForArbeidUnderAAPDto(fastsattPrMnd));
        return faktaBeregningLagreDto;
    }

    private static Optional<BeregningsgrunnlagGrunnlagEntitet> lagForrigeBeregningsgrunnlag(BigDecimal beregnetPrÅr) {
        var forrigeBg = BeregningsgrunnlagEntitet.ny().medGrunnbeløp(new Beløp(600000)).medSkjæringstidspunkt(STP).build();
        var periode = BeregningsgrunnlagPeriode.ny().medBeregningsgrunnlagPeriode(STP.minusMonths(1), STP.plusMonths(2).minusDays(1)).build(forrigeBg);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medArbforholdType(OpptjeningAktivitetType.ARBEID_UNDER_AAP)
            .medBeregnetPrÅr(beregnetPrÅr)
            .build(periode);
        return Optional.of(BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(forrigeBg)
            .build(1L, BeregningsgrunnlagTilstand.OPPRETTET));
    }
}
