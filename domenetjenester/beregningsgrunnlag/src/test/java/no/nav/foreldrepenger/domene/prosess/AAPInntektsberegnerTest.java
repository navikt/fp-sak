package no.nav.foreldrepenger.domene.prosess;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Beløp;

class AAPInntektsberegnerTest {
    private static final LocalDate STP = LocalDate.of(2025,4,26);
    private static final AktørId AKTØR = new AktørId("0000000000000");
    private final InntektArbeidYtelseAggregatBuilder data = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
    private final InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder inntektBuilder = data.getAktørInntektBuilder(AKTØR);


    @Test
    void skal_teste_ingen_inntekt() {
        // Act
        var inntektSum = AAPInntektsberegner.finnAllBeregnetInntektIBeregningsperioden(Optional.of(inntektBuilder.build()), STP);

        // Assert
        assertThat(inntektSum).isEqualTo(Beløp.ZERO);
    }

    @Test
    void skal_teste_inntekt_hos_en_arbeidsgiver() {
        // Arrange
        lagInntekt(Arbeidsgiver.virksomhet("999999999"), STP.minusMonths(6), 6, 20_000);

        // Act
        var inntektSum = AAPInntektsberegner.finnAllBeregnetInntektIBeregningsperioden(Optional.of(inntektBuilder.build()), STP);

        // Assert
        assertThat(inntektSum).isEqualTo(Beløp.fra(BigDecimal.valueOf(60_000)));
    }

    @Test
    void skal_teste_delvis_inntekt_hos_en_arbeidsgiver() {
        // Arrange
        lagInntekt(Arbeidsgiver.virksomhet("999999999"), STP.minusMonths(3), 1, 20_000);

        // Act
        var inntektSum = AAPInntektsberegner.finnAllBeregnetInntektIBeregningsperioden(Optional.of(inntektBuilder.build()), STP);

        // Assert
        assertThat(inntektSum).isEqualTo(Beløp.fra(BigDecimal.valueOf(20_000)));
    }

    @Test
    void skal_beregne_inntekt_flere_arbeidsgivere() {
        // Arrange
        lagInntekt(Arbeidsgiver.virksomhet("999999999"), STP.minusMonths(6), 6, 30_000);
        lagInntekt(Arbeidsgiver.virksomhet("999999998"), STP.minusMonths(3), 2, 10_000);

        // Act
        var inntektSum = AAPInntektsberegner.finnAllBeregnetInntektIBeregningsperioden(Optional.of(inntektBuilder.build()), STP);

        // Assert
        assertThat(inntektSum).isEqualTo(Beløp.fra(BigDecimal.valueOf(110_000)));
    }

    private void lagInntekt(Arbeidsgiver ag, LocalDate fom, int måneder, int inntekt) {
        var intBuilder = InntektBuilder.oppdatere(Optional.empty());
        intBuilder.medArbeidsgiver(ag).medInntektsKilde(InntektsKilde.INNTEKT_BEREGNING);
        for (var i = 0; i < måneder; i++) {
            var start = fom.plusMonths(i);
            var postBuilder = intBuilder.getInntektspostBuilder();
            postBuilder.medPeriode(start.withDayOfMonth(1), start.with(TemporalAdjusters.lastDayOfMonth()))
                .medBeløp(BigDecimal.valueOf(inntekt))
                .medInntektspostType(InntektspostType.LØNN);
            intBuilder.leggTilInntektspost(postBuilder);
        }
        inntektBuilder.leggTilInntekt(intBuilder);
    }

}
