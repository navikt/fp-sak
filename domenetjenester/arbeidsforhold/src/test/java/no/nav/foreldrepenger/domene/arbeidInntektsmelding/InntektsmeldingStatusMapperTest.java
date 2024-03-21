package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class InntektsmeldingStatusMapperTest {

    @Test
    void skal_teste_alle_im_mottatt() {
        var ag = Arbeidsgiver.virksomhet("999999999");
        var ref = InternArbeidsforholdRef.nyRef();

        var arbeidsforholdInntektsmeldinger = InntektsmeldingStatusMapper.mapInntektsmeldingStatus(Map.of(ag, Set.of(ref)), Map.of(), Collections.emptyList());

        assertThat(arbeidsforholdInntektsmeldinger).isNotNull().hasSize(1);
        assertThat(arbeidsforholdInntektsmeldinger.getFirst().inntektsmeldingStatus()).isEqualTo(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT);
        assertThat(arbeidsforholdInntektsmeldinger.getFirst().arbeidsgiver()).isEqualTo(ag);
    }

    @Test
    void skal_teste_im_mangler() {
        var ag = Arbeidsgiver.virksomhet("999999999");
        var ref = InternArbeidsforholdRef.nyRef();

        var arbeidsforholdInntektsmeldinger = InntektsmeldingStatusMapper.mapInntektsmeldingStatus(Map.of(ag, Set.of(ref)), Map.of(ag, Set.of(ref)), Collections.emptyList());

        assertThat(arbeidsforholdInntektsmeldinger).isNotNull().hasSize(1);
        assertThat(arbeidsforholdInntektsmeldinger.getFirst().inntektsmeldingStatus()).isEqualTo(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT);
        assertThat(arbeidsforholdInntektsmeldinger.getFirst().arbeidsgiver()).isEqualTo(ag);
    }

    @Test
    void skal_teste_en_im_mangler_en_er_mottatt() {
        var ag = Arbeidsgiver.virksomhet("999999999");
        var ref1 = InternArbeidsforholdRef.nyRef();
        var ref2 = InternArbeidsforholdRef.nyRef();

        var arbeidsforholdInntektsmeldinger = InntektsmeldingStatusMapper.mapInntektsmeldingStatus(Map.of(ag, Set.of(ref1, ref2)), Map.of(ag, Set.of(ref2)), Collections.emptyList());

        assertThat(arbeidsforholdInntektsmeldinger).isNotNull().hasSize(2);
        var mottattIM = arbeidsforholdInntektsmeldinger.stream()
            .filter(ai -> ai.ref().gjelderFor(ref1))
            .findFirst()
            .orElseThrow();
        assertThat(mottattIM.inntektsmeldingStatus()).isEqualTo(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT);
        var ikkeMottattIM = arbeidsforholdInntektsmeldinger.stream()
            .filter(ai -> ai.ref().gjelderFor(ref2))
            .findFirst()
            .orElseThrow();
        assertThat(ikkeMottattIM.inntektsmeldingStatus()).isEqualTo(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT);
    }
}
