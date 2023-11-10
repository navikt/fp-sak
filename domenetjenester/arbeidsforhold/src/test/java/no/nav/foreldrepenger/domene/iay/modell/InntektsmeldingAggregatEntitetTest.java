package no.nav.foreldrepenger.domene.iay.modell;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

class InntektsmeldingAggregatEntitetTest {

    private static final String ORGNR = "123";

    @Test
    void skal_lagre_i_riktig_rekkefølge() {
        var nå = LocalDateTime.now();
        var arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

        var førsteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        førsteInntektsmeldingBuilder.medKanalreferanse("AR123")
                .medArbeidsgiver(arbeidsgiver).medInnsendingstidspunkt(nå);

        var sisteInntektsmedlingBuilder = InntektsmeldingBuilder.builder();
        sisteInntektsmedlingBuilder.medKanalreferanse("AR124")
                .medArbeidsgiver(arbeidsgiver).medInnsendingstidspunkt(nå);

        var inntektsmeldingAggregat = new InntektsmeldingAggregat();
        inntektsmeldingAggregat.leggTil(førsteInntektsmeldingBuilder.build());
        inntektsmeldingAggregat.leggTil(sisteInntektsmedlingBuilder.build());

        var inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();
        assertThat(inntektsmeldinger).hasSize(1);
        assertThat(inntektsmeldinger.get(0).getKanalreferanse()).isEqualTo("AR124");
    }

    @Test
    void skal_bruk_ar_hvis_altinn_involvert() {
        var nå = LocalDateTime.now();
        var arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

        var førsteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        førsteInntektsmeldingBuilder.medKanalreferanse("AR123").medKildesystem("U4BW")
                .medArbeidsgiver(arbeidsgiver).medInnsendingstidspunkt(nå);

        var sisteInntektsmedlingBuilder = InntektsmeldingBuilder.builder();
        sisteInntektsmedlingBuilder.medKanalreferanse("AR124").medKildesystem("AltinnPortal")
                .medArbeidsgiver(arbeidsgiver).medInnsendingstidspunkt(nå.minusMinutes(2));

        var inntektsmeldingAggregat = new InntektsmeldingAggregat();
        inntektsmeldingAggregat.leggTil(førsteInntektsmeldingBuilder.build());
        inntektsmeldingAggregat.leggTil(sisteInntektsmedlingBuilder.build());

        var inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();
        assertThat(inntektsmeldinger).hasSize(1);
        assertThat(inntektsmeldinger.get(0).getKanalreferanse()).isEqualTo("AR124");
    }

    @Test
    void skal_ikke_lagre_når_eldre_kanalreferanse_kommer_inn_med_lik_innsendingstidspunkt() {
        var nå = LocalDateTime.now();
        var arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

        var sisteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        sisteInntektsmeldingBuilder.medKanalreferanse("AR125")
                .medArbeidsgiver(arbeidsgiver).medInnsendingstidspunkt(nå);

        var førsteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        førsteInntektsmeldingBuilder.medKanalreferanse("AR124")
                .medArbeidsgiver(arbeidsgiver).medInnsendingstidspunkt(nå);

        var inntektsmeldingAggregat = new InntektsmeldingAggregat();
        inntektsmeldingAggregat.leggTil(sisteInntektsmeldingBuilder.build());
        inntektsmeldingAggregat.leggTil(førsteInntektsmeldingBuilder.build());

        var inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();
        assertThat(inntektsmeldinger).hasSize(1);
        assertThat(inntektsmeldinger.get(0).getKanalreferanse()).isEqualTo("AR125");
    }

    @Test
    void skal_tolke_null_i_kanalreferanse_som_gammel_ved_like_innsendingstidspunkt() {
        var nå = LocalDateTime.now();
        var arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

        var sisteInntektsmeldingBuilder = InntektsmeldingBuilder.builder()
                .medArbeidsgiver(arbeidsgiver)
                .medInnsendingstidspunkt(nå);

        var førsteInntektsmeldingBuilder = InntektsmeldingBuilder.builder()
                .medKanalreferanse("AR124")
                .medArbeidsgiver(arbeidsgiver)
                .medInnsendingstidspunkt(nå);

        var inntektsmeldingAggregat = new InntektsmeldingAggregat();
        inntektsmeldingAggregat.leggTil(sisteInntektsmeldingBuilder.build());
        inntektsmeldingAggregat.leggTil(førsteInntektsmeldingBuilder.build());

        var inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();
        assertThat(inntektsmeldinger).hasSize(1);
        assertThat(inntektsmeldinger.get(0).getKanalreferanse()).isEqualTo("AR124");
    }

    @Test
    void skal_benytte_innsendingstidpunkt_hvis_de_er_ulike() {
        var nå = LocalDateTime.now();
        var omTi = LocalDateTime.now().plusMinutes(10);
        var arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

        var sisteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        sisteInntektsmeldingBuilder
                .medKanalreferanse("AR124")
                .medArbeidsgiver(arbeidsgiver)
                .medInnsendingstidspunkt(omTi);

        var førsteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        førsteInntektsmeldingBuilder
                .medKanalreferanse("AR125")
                .medArbeidsgiver(arbeidsgiver)
                .medInnsendingstidspunkt(nå);

        var inntektsmeldingAggregat = new InntektsmeldingAggregat();
        inntektsmeldingAggregat.leggTil(sisteInntektsmeldingBuilder.build());
        inntektsmeldingAggregat.leggTil(førsteInntektsmeldingBuilder.build());

        var inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();
        assertThat(inntektsmeldinger).hasSize(1);
        assertThat(inntektsmeldinger.get(0).getKanalreferanse()).isEqualTo("AR124");
    }
}
