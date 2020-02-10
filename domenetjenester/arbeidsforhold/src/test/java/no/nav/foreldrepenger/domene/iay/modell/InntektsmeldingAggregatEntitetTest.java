package no.nav.foreldrepenger.domene.iay.modell;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.vedtak.util.FPDateUtil;

public class InntektsmeldingAggregatEntitetTest {

    private static final String ORGNR = "123";

    @Test
    public void skal_lagre_i_riktig_rekkefølge() {
        LocalDateTime nå = FPDateUtil.nå();
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

        InntektsmeldingBuilder førsteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        førsteInntektsmeldingBuilder.medKanalreferanse("AR123")
        .medArbeidsgiver(arbeidsgiver).medInnsendingstidspunkt(nå);

        InntektsmeldingBuilder sisteInntektsmedlingBuilder = InntektsmeldingBuilder.builder();
        sisteInntektsmedlingBuilder.medKanalreferanse("AR124")
            .medArbeidsgiver(arbeidsgiver).medInnsendingstidspunkt(nå);

        InntektsmeldingAggregat inntektsmeldingAggregat = new InntektsmeldingAggregat();
        inntektsmeldingAggregat.leggTil(førsteInntektsmeldingBuilder.build());
        inntektsmeldingAggregat.leggTil(sisteInntektsmedlingBuilder.build());

        List<Inntektsmelding> inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();
        assertThat(inntektsmeldinger).hasSize(1);
        assertThat(inntektsmeldinger.get(0).getKanalreferanse()).isEqualTo("AR124");
    }

    @Test
    public void skal_bruk_ar_hvis_altinn_involvert() {
        LocalDateTime nå = FPDateUtil.nå();
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

        InntektsmeldingBuilder førsteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        førsteInntektsmeldingBuilder.medKanalreferanse("AR123").medKildesystem("U4BW")
            .medArbeidsgiver(arbeidsgiver).medInnsendingstidspunkt(nå);

        InntektsmeldingBuilder sisteInntektsmedlingBuilder = InntektsmeldingBuilder.builder();
        sisteInntektsmedlingBuilder.medKanalreferanse("AR124").medKildesystem("AltinnPortal")
            .medArbeidsgiver(arbeidsgiver).medInnsendingstidspunkt(nå.minusMinutes(2));

        InntektsmeldingAggregat inntektsmeldingAggregat = new InntektsmeldingAggregat();
        inntektsmeldingAggregat.leggTil(førsteInntektsmeldingBuilder.build());
        inntektsmeldingAggregat.leggTil(sisteInntektsmedlingBuilder.build());

        List<Inntektsmelding> inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();
        assertThat(inntektsmeldinger).hasSize(1);
        assertThat(inntektsmeldinger.get(0).getKanalreferanse()).isEqualTo("AR124");
    }

    @Test
    public void skal_ikke_lagre_når_eldre_kanalreferanse_kommer_inn_med_lik_innsendingstidspunkt() {
        LocalDateTime nå = FPDateUtil.nå();
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

        InntektsmeldingBuilder sisteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        sisteInntektsmeldingBuilder.medKanalreferanse("AR125")
            .medArbeidsgiver(arbeidsgiver).medInnsendingstidspunkt(nå);

        InntektsmeldingBuilder førsteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        førsteInntektsmeldingBuilder.medKanalreferanse("AR124")
            .medArbeidsgiver(arbeidsgiver).medInnsendingstidspunkt(nå);


        InntektsmeldingAggregat inntektsmeldingAggregat = new InntektsmeldingAggregat();
        inntektsmeldingAggregat.leggTil(sisteInntektsmeldingBuilder.build());
        inntektsmeldingAggregat.leggTil(førsteInntektsmeldingBuilder.build());


        List<Inntektsmelding> inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();
        assertThat(inntektsmeldinger).hasSize(1);
        assertThat(inntektsmeldinger.get(0).getKanalreferanse()).isEqualTo("AR125");
    }

    @Test
    public void skal_tolke_null_i_kanalreferanse_som_gammel_ved_like_innsendingstidspunkt() {
        LocalDateTime nå = FPDateUtil.nå();
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

        InntektsmeldingBuilder sisteInntektsmeldingBuilder = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medInnsendingstidspunkt(nå);

        InntektsmeldingBuilder førsteInntektsmeldingBuilder = InntektsmeldingBuilder.builder()
            .medKanalreferanse("AR124")
            .medArbeidsgiver(arbeidsgiver)
            .medInnsendingstidspunkt(nå);

        InntektsmeldingAggregat inntektsmeldingAggregat = new InntektsmeldingAggregat();
        inntektsmeldingAggregat.leggTil(sisteInntektsmeldingBuilder.build());
        inntektsmeldingAggregat.leggTil(førsteInntektsmeldingBuilder.build());


        List<Inntektsmelding> inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();
        assertThat(inntektsmeldinger).hasSize(1);
        assertThat(inntektsmeldinger.get(0).getKanalreferanse()).isEqualTo("AR124");
    }

    @Test
    public void skal_benytte_innsendingstidpunkt_hvis_de_er_ulike() {
        LocalDateTime nå = FPDateUtil.nå();
        LocalDateTime omTi = FPDateUtil.nå().plusMinutes(10);
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);

        InntektsmeldingBuilder sisteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        sisteInntektsmeldingBuilder
            .medKanalreferanse("AR124")
            .medArbeidsgiver(arbeidsgiver)
            .medInnsendingstidspunkt(omTi);

        InntektsmeldingBuilder førsteInntektsmeldingBuilder = InntektsmeldingBuilder.builder();
        førsteInntektsmeldingBuilder
            .medKanalreferanse("AR125")
            .medArbeidsgiver(arbeidsgiver)
            .medInnsendingstidspunkt(nå);

        InntektsmeldingAggregat inntektsmeldingAggregat = new InntektsmeldingAggregat();
        inntektsmeldingAggregat.leggTil(sisteInntektsmeldingBuilder.build());
        inntektsmeldingAggregat.leggTil(førsteInntektsmeldingBuilder.build());

        List<Inntektsmelding> inntektsmeldinger = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();
        assertThat(inntektsmeldinger).hasSize(1);
        assertThat(inntektsmeldinger.get(0).getKanalreferanse()).isEqualTo("AR124");
    }
}
