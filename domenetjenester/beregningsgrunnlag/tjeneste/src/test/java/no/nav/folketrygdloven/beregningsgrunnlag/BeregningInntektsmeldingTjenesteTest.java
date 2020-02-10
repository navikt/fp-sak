package no.nav.folketrygdloven.beregningsgrunnlag;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import no.nav.folketrygdloven.kalkulator.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.kalkulator.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.kalkulator.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.kalkulator.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.kalkulator.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;

public class BeregningInntektsmeldingTjenesteTest {

    public static final LocalDate STP = LocalDate.now();
    public static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet(KUNSTIG_ORG);

    @Test
    public void skalFinneRefusjonskravForOpphørsdato() {
        // Arrange
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER))
            .medSkjæringstidspunkt(STP)
            .build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(STP, STP)
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel andel = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(ARBEIDSGIVER))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode);
        BigDecimal refusjonFraIM = BigDecimal.valueOf(1337);
        Inntektsmelding im = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(ARBEIDSGIVER)
            .medRefusjon(refusjonFraIM, STP)
            .build();

        // Act
        Optional<BigDecimal> refusjon = BeregningInntektsmeldingTjeneste.finnRefusjonskravPrÅrIPeriodeForAndel(andel, periode.getPeriode(), List.of(im));

        // Assert
        assertThat(refusjon).isPresent();
        assertThat(refusjon.get()).isEqualByComparingTo(refusjonFraIM.multiply(BigDecimal.valueOf(12)));

    }
}
