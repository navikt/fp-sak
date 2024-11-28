package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;


import static no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus.BRUKERS_ANDEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsattBrukersAndel;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBgKunYtelseDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.domene.typer.Beløp;

class KunYtelseHistorikkTjenesteTest {
    private static final Long ANDELSNR = 1L;
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static final Beløp GRUNNBELØP = new Beløp(600000);
    private static final String ANDELSINFO = "Brukers andel";

    private static final ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste = mock(ArbeidsgiverHistorikkinnslag.class);

    private KunYtelseHistorikkTjeneste kunYtelseHistorikkTjeneste;
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    @BeforeEach
    public void setup() {
        kunYtelseHistorikkTjeneste = new KunYtelseHistorikkTjeneste(arbeidsgiverHistorikkinnslagTjeneste);
        beregningsgrunnlag = BeregningsgrunnlagEntitet.ny().medGrunnbeløp(GRUNNBELØP).medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        var periode1 = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder().medAndelsnr(ANDELSNR).medAktivitetStatus(BRUKERS_ANDEL).build(periode1);

        when(arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(any(), any(), any(), anyList())).thenReturn(
            ANDELSINFO);
    }

    @Test
    void skal_lage_historikk_for_andel_som_eksisterte_fra_før_i_grunnlag_ved_første_utførelse_av_aksjonspunkt() {
        // Arrange
        var nyAndel = false;
        var lagtTilAvSaksbehandler = false;
        var fastsatt = 100000;
        var inntektskategori = Inntektskategori.SJØMANN;
        var andel = new FastsattBrukersAndel(nyAndel, ANDELSNR, lagtTilAvSaksbehandler, fastsatt, inntektskategori);
        var kunYtelseDto = new FastsettBgKunYtelseDto(Collections.singletonList(andel), null);
        var dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE), kunYtelseDto);

        // Act
        var historikkTekstlinjeBuilder = kunYtelseHistorikkTjeneste.lagHistorikk(dto, beregningsgrunnlag, Optional.empty(),
            InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        var faktiskeTekstlinjer = historikkTekstlinjeBuilder.stream().map(HistorikkinnslagTekstlinjeBuilder::build);
        var forventetTekstlinjer = List.of(new HistorikkinnslagTekstlinjeBuilder().tekst("Fordeling for __" + ANDELSINFO + "__:").build(),
            new HistorikkinnslagTekstlinjeBuilder().tekst(" __" + inntektskategori.getNavn() + "__ er satt til __" + fastsatt + "__").build(),
            new HistorikkinnslagTekstlinjeBuilder().tekst(
                " __Inntektskategori for " + ANDELSINFO + "__ er satt til __" + inntektskategori.getNavn() + "__").build());

        // Assert
        assertThat(historikkTekstlinjeBuilder).isNotNull();
        assertThat(faktiskeTekstlinjer).containsAnyElementsOf(forventetTekstlinjer);
    }

    @Test
    void skal_lage_historikk_for_andel_som_eksisterte_fra_før_i_grunnlag() {
        // Arrange
        var nyAndel = false;
        var lagtTilAvSaksbehandler = false;
        var fastsatt = 100000;
        var inntektskategori = Inntektskategori.ARBEIDSTAKER;
        var brukersAndel = new FastsattBrukersAndel(nyAndel, ANDELSNR, lagtTilAvSaksbehandler, fastsatt, inntektskategori);
        var kunYtelseDto = new FastsettBgKunYtelseDto(Collections.singletonList(brukersAndel), null);
        var dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE), kunYtelseDto);
        var forrigeInntektskategori = Inntektskategori.FRILANSER;

        var forrigeBg = new BeregningsgrunnlagEntitet(beregningsgrunnlag);
        forrigeBg.getBeregningsgrunnlagPerioder()
            .forEach(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList()
                .forEach(andel -> BeregningsgrunnlagPrStatusOgAndel.builder(andel)
                    .medBeregnetPrÅr(BigDecimal.valueOf(fastsatt * 8))
                    .medInntektskategori(forrigeInntektskategori)));

        var forrigeGrunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(forrigeBg)
            .build(1L, BeregningsgrunnlagTilstand.KOFAKBER_UT);

        // Act
        var historikkTekstlinjeBuilder = kunYtelseHistorikkTjeneste.lagHistorikk(dto, beregningsgrunnlag, Optional.of(forrigeGrunnlag),
            InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        var faktiskeTekstlinjer = historikkTekstlinjeBuilder.stream().map(HistorikkinnslagTekstlinjeBuilder::build);
        var forventetTekstlinjer = List.of(new HistorikkinnslagTekstlinjeBuilder().tekst("Fordeling for __" + ANDELSINFO + "__:").build(),
            new HistorikkinnslagTekstlinjeBuilder().tekst(
                " __" + forrigeInntektskategori.getNavn() + "__ er endret fra 66667 til __" + fastsatt + "__").build(),
            new HistorikkinnslagTekstlinjeBuilder().tekst(
                " __Inntektskategori for" + ANDELSINFO + "__ er endret fra " + forrigeInntektskategori.getNavn() + " til __"
                    + inntektskategori.getNavn() + "__").build());

        // Assert
        assertThat(historikkTekstlinjeBuilder).isNotNull();
        assertThat(faktiskeTekstlinjer).containsAnyElementsOf(forventetTekstlinjer);
    }

}
