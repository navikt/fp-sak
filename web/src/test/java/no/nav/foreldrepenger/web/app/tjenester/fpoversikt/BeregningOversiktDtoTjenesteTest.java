package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;

import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeregningOversiktDtoTjenesteTest {

    @Mock
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    @Mock
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    @Mock
    private BeregningTjeneste beregningTjeneste;

    private BeregningOversiktDtoTjeneste beregningOversiktDtoTjeneste;

    @BeforeEach
    void setUp() {
        beregningOversiktDtoTjeneste = new BeregningOversiktDtoTjeneste(beregningTjeneste, inntektArbeidYtelseTjeneste, arbeidsgiverTjeneste);
    }

    @Test
    void dto_med_ett_arbeidsforhold() {
        var beregningsgrunnlag = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(LocalDate.now())
            .medGrunnbeløp(BigDecimal.TEN)
            .leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.builder()
                .medBeregningsgrunnlagPeriode(LocalDate.now(), LocalDate.now())
                .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                    .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(Arbeidsgiver.virksomhet("9".repeat(9))).build())
                    .medBeregnetPrÅr(BigDecimal.TEN)
                    .medKilde(AndelKilde.PROSESS_START)
                    .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                    .build())
                .build())
            .build();

        var grunnlagBeregningsgrunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt()
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(BeregningsgrunnlagTilstand.FASTSATT);

        var iaygr = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medInntektsmeldinger(
                List.of(InntektsmeldingBuilder.builder().medArbeidsgiver(Arbeidsgiver.virksomhet("9".repeat(9))).medBeløp(BigDecimal.TEN).build()))
            .build();

        when(inntektArbeidYtelseTjeneste.finnGrunnlag(any())).thenReturn(Optional.of(iaygr));
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(grunnlagBeregningsgrunnlag));
        when(arbeidsgiverTjeneste.hent(any())).thenReturn(new ArbeidsgiverOpplysninger("9".repeat(9), "Testbedriften"));

        var ref = BehandlingReferanse.fra(ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked());
        var dto = beregningOversiktDtoTjeneste.lagDtoForBehandling(ref);

        assertThat(dto).isPresent();
        assertThat(dto.get().grunnbeløp()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(dto.get().skjæringstidspunkt()).isEqualTo(LocalDate.now());
        assertThat(dto.get().beregningsAndeler()).hasSize(1);
        assertThat(dto.get().beregningsAndeler().getFirst().arbeidsforhold().arbeidsgiverIdent()).isEqualTo("9".repeat(9));
        assertThat(dto.get().beregningsAndeler().getFirst().arbeidsforhold().arbeidsgivernavn()).isEqualTo("Testbedriften");
        assertThat(dto.get().beregningsAndeler().getFirst().fastsattPrÅr()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(dto.get().beregningsAndeler().getFirst().inntektsKilde()).isEqualTo(FpSak.Beregningsgrunnlag.InntektsKilde.INNTEKTSMELDING);
    }

    @Test
    void dto_med_to_andeler_ingen_arbeidsforhold() {
        var beregningsgrunnlag = Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(LocalDate.now())
            .medGrunnbeløp(BigDecimal.TEN)
            .leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.builder()
                .medBeregningsgrunnlagPeriode(LocalDate.now(), LocalDate.now())
                .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                    .medBeregnetPrÅr(BigDecimal.TEN)
                    .medAndelsnr(1L)
                    .medKilde(AndelKilde.PROSESS_START)
                    .medAktivitetStatus(AktivitetStatus.FRILANSER)
                    .build())
                .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                    .medBeregnetPrÅr(BigDecimal.TWO)
                    .medAndelsnr(2L)
                    .medKilde(AndelKilde.PROSESS_START)
                    .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
                    .build())
                .build())
            .build();

        var grunnlagBeregningsgrunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt()
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .build(BeregningsgrunnlagTilstand.FASTSATT);

        when(inntektArbeidYtelseTjeneste.finnGrunnlag(any())).thenReturn(Optional.empty());
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(grunnlagBeregningsgrunnlag));

        var ref = BehandlingReferanse.fra(ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked());
        var dto = beregningOversiktDtoTjeneste.lagDtoForBehandling(ref);

        assertThat(dto).isPresent();
        assertThat(dto.get().grunnbeløp()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(dto.get().skjæringstidspunkt()).isEqualTo(LocalDate.now());
        assertThat(dto.get().beregningsAndeler()).hasSize(2);

        var frilansAndel = dto.get()
            .beregningsAndeler()
            .stream()
            .filter(a -> a.aktivitetStatus().equals(FpSak.Beregningsgrunnlag.AktivitetStatus.FRILANSER))
            .findFirst()
            .orElseThrow();
        assertThat(frilansAndel.fastsattPrÅr()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(frilansAndel.inntektsKilde()).isEqualTo(FpSak.Beregningsgrunnlag.InntektsKilde.A_INNTEKT);

        var SnAndel = dto.get()
            .beregningsAndeler()
            .stream()
            .filter(a -> a.aktivitetStatus().equals(FpSak.Beregningsgrunnlag.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE))
            .findFirst()
            .orElseThrow();
        assertThat(SnAndel.fastsattPrÅr()).isEqualByComparingTo(BigDecimal.TWO);
        assertThat(SnAndel.inntektsKilde()).isEqualTo(FpSak.Beregningsgrunnlag.InntektsKilde.PENSJONSGIVENDE_INNTEKT);
    }


}
