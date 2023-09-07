package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.typer.Beløp;

class InntektsmeldingDtoTjenesteTest {

    @Test
    void henter_im() {
        var iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var tjeneste = new InntektsmeldingDtoTjeneste(new InntektsmeldingTjeneste(iayTjeneste));

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        var innsendingstidspunkt = LocalDateTime.now();
        var inntekt = new Beløp(400000);
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var imBuilder = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medBeløp(inntekt.getVerdi())
            .medInnsendingstidspunkt(innsendingstidspunkt);
        iayTjeneste.lagreInntektsmeldinger(behandling.getFagsak().getSaksnummer(), behandling.getId(), List.of(imBuilder));

        var inntektsmeldingerForSak = tjeneste.hentInntektsmeldingerForSak(behandling.getFagsak().getSaksnummer());

        assertThat(inntektsmeldingerForSak).hasSize(1);
        var im = inntektsmeldingerForSak.stream().findFirst().get();
        assertThat(im.arbeidsgiver().identifikator()).isEqualTo(arbeidsgiver.getIdentifikator());
        assertThat(im.innsendingstidspunkt()).isEqualTo(innsendingstidspunkt);
        assertThat(im.inntekt()).isEqualTo(inntekt.getVerdi());
    }
}
