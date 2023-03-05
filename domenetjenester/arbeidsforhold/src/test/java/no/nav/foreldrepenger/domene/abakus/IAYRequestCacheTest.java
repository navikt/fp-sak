package no.nav.foreldrepenger.domene.abakus;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;

class IAYRequestCacheTest {

    @Test
    void skal_levere_ut_siste_grunnlag_ref() throws InterruptedException {

        var cache = new IAYRequestCache();

        final var koblingReferanse = UUID.randomUUID();
        final var grunnlag1 = new AbakusInntektArbeidYtelseGrunnlag(InntektArbeidYtelseGrunnlagBuilder.nytt().build(), koblingReferanse);

        cache.leggTil(grunnlag1);
        assertThat(cache.getSisteAktiveGrunnlagReferanse(grunnlag1.getKoblingReferanse().orElse(null))).isEqualTo(grunnlag1.getEksternReferanse());

        Thread.sleep(10);

        final var grunnlag2 = new AbakusInntektArbeidYtelseGrunnlag(InntektArbeidYtelseGrunnlagBuilder.nytt().build(), koblingReferanse);

        cache.leggTil(grunnlag2);
        assertThat(cache.getSisteAktiveGrunnlagReferanse(grunnlag2.getKoblingReferanse().orElse(null))).isEqualTo(grunnlag2.getEksternReferanse());
    }
}
