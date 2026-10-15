package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class InntektsmeldingStatusMapperTest {

    @Test
    void skal_teste_alle_im_mottatt() {
        var ag = Arbeidsgiver.virksomhet("999999999");

        var arbeidsforholdInntektsmeldinger = InntektsmeldingStatusMapper.mapInntektsmeldingStatus(Set.of(ag), Set.of(), Collections.emptyList());

        assertThat(arbeidsforholdInntektsmeldinger).isNotNull().hasSize(1);
        assertThat(arbeidsforholdInntektsmeldinger.getFirst().inntektsmeldingStatus()).isEqualTo(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT);
        assertThat(arbeidsforholdInntektsmeldinger.getFirst().arbeidsgiver()).isEqualTo(ag);
    }

    @Test
    void skal_teste_im_mangler() {
        var ag = Arbeidsgiver.virksomhet("999999999");

        var arbeidsforholdInntektsmeldinger = InntektsmeldingStatusMapper.mapInntektsmeldingStatus(Set.of(ag), Set.of(ag), Collections.emptyList());

        assertThat(arbeidsforholdInntektsmeldinger).isNotNull().hasSize(1);
        assertThat(arbeidsforholdInntektsmeldinger.getFirst().inntektsmeldingStatus()).isEqualTo(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT);
        assertThat(arbeidsforholdInntektsmeldinger.getFirst().arbeidsgiver()).isEqualTo(ag);
    }

    @Test
    void skal_teste_en_im_mangler_en_er_mottatt() {
        var ag1 = Arbeidsgiver.virksomhet("999999999");
        var ag2 = Arbeidsgiver.virksomhet("888888888");

        var arbeidsforholdInntektsmeldinger = InntektsmeldingStatusMapper.mapInntektsmeldingStatus(Set.of(ag1, ag2), Set.of(ag2), Collections.emptyList());

        assertThat(arbeidsforholdInntektsmeldinger).extracting(ArbeidsforholdInntektsmeldingStatus::arbeidsgiver).containsExactly(ag2, ag1);
        assertThat(finnStatus(arbeidsforholdInntektsmeldinger, ag1)).isEqualTo(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT);
        assertThat(finnStatus(arbeidsforholdInntektsmeldinger, ag2)).isEqualTo(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT);
    }

    @Test
    void skal_gi_avklart_når_saksbehandler_har_valgt_å_fortsette_uten_inntektsmelding() {
        var ag = Arbeidsgiver.virksomhet("999999999");
        var valg = lagValg(ag, InternArbeidsforholdRef.nullRef(), ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING);

        var arbeidsforholdInntektsmeldinger = InntektsmeldingStatusMapper.mapInntektsmeldingStatus(Set.of(ag), Set.of(ag), List.of(valg));

        assertThat(finnStatus(arbeidsforholdInntektsmeldinger, ag)).isEqualTo(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.AVKLART_IKKE_PÅKREVD);
    }

    @Test
    void eldre_valg_lagret_på_arbeidsforhold_gjelder_hele_arbeidsgiveren() {
        var ag = Arbeidsgiver.virksomhet("999999999");
        var valgAf1 = lagValg(ag, InternArbeidsforholdRef.nyRef(), ArbeidsforholdKomplettVurderingType.KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING);
        var valgAf2 = lagValg(ag, InternArbeidsforholdRef.nyRef(), ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING);

        var arbeidsforholdInntektsmeldinger = InntektsmeldingStatusMapper.mapInntektsmeldingStatus(Set.of(ag), Set.of(ag), List.of(valgAf1, valgAf2));

        assertThat(arbeidsforholdInntektsmeldinger).hasSize(1);
        assertThat(finnStatus(arbeidsforholdInntektsmeldinger, ag)).isEqualTo(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.AVKLART_IKKE_PÅKREVD);
    }

    @Test
    void valg_for_annen_arbeidsgiver_påvirker_ikke_status() {
        var ag = Arbeidsgiver.virksomhet("999999999");
        var annenAg = Arbeidsgiver.virksomhet("888888888");
        var valg = lagValg(annenAg, InternArbeidsforholdRef.nullRef(), ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING);

        var arbeidsforholdInntektsmeldinger = InntektsmeldingStatusMapper.mapInntektsmeldingStatus(Set.of(ag), Set.of(ag), List.of(valg));

        assertThat(finnStatus(arbeidsforholdInntektsmeldinger, ag)).isEqualTo(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT);
    }

    private static ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus finnStatus(List<ArbeidsforholdInntektsmeldingStatus> statuser, Arbeidsgiver ag) {
        return statuser.stream().filter(s -> s.arbeidsgiver().equals(ag)).findFirst().orElseThrow().inntektsmeldingStatus();
    }

    private static ArbeidsforholdValg lagValg(Arbeidsgiver ag, InternArbeidsforholdRef ref, ArbeidsforholdKomplettVurderingType vurdering) {
        return ArbeidsforholdValg.builder()
            .medArbeidsgiver(ag.getIdentifikator())
            .medArbeidsforholdRef(ref)
            .medVurdering(vurdering)
            .medBegrunnelse("begrunnelse")
            .build();
    }
}
