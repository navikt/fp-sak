package no.nav.foreldrepenger.web.app.tjenester.formidling;

import static no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.AVKLART_IKKE_PÅKREVD;
import static no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT;
import static no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.kontrakter.fpsak.inntektsmeldinger.ArbeidsforholdInntektsmeldingerDto.ArbeidsforholdInntektsmeldingDto;

class BrevGrunnlagArbeidsforholdInntektsmeldingTjenesteTest {

    private static final Arbeidsgiver AG = Arbeidsgiver.virksomhet("999999999");

    @Test
    void skal_teste_alle_im_mottatt() {
        var resultat = BrevGrunnlagArbeidsforholdInntektsmeldingTjeneste.mapInntektsmeldingStatus(
            List.of(status(AG, MOTTATT)));

        assertThat(resultat.arbeidsforholdInntektsmelding()).containsExactly(new ArbeidsforholdInntektsmeldingDto(AG.getIdentifikator(), true));
    }

    @Test
    void skal_teste_im_mangler() {
        var resultat = BrevGrunnlagArbeidsforholdInntektsmeldingTjeneste.mapInntektsmeldingStatus(
            List.of(status(AG, IKKE_MOTTAT)));

        assertThat(resultat.arbeidsforholdInntektsmelding()).containsExactly(new ArbeidsforholdInntektsmeldingDto(AG.getIdentifikator(), false));
    }

    @Test
    void skal_gi_en_linje_per_arbeidsgiver_med_flere_arbeidsforhold() {
        var resultat = BrevGrunnlagArbeidsforholdInntektsmeldingTjeneste.mapInntektsmeldingStatus(
            List.of(status(AG, IKKE_MOTTAT), status(AG, IKKE_MOTTAT)));

        assertThat(resultat.arbeidsforholdInntektsmelding()).containsExactly(new ArbeidsforholdInntektsmeldingDto(AG.getIdentifikator(), false));
    }

    @Test
    void skal_regne_arbeidsgiver_som_ikke_mottatt_når_ett_arbeidsforhold_mangler_og_ett_er_mottatt() {
        var resultat = BrevGrunnlagArbeidsforholdInntektsmeldingTjeneste.mapInntektsmeldingStatus(
            List.of(status(AG, MOTTATT), status(AG, IKKE_MOTTAT)));

        assertThat(resultat.arbeidsforholdInntektsmelding()).containsExactly(new ArbeidsforholdInntektsmeldingDto(AG.getIdentifikator(), false));
    }

    @Test
    void skal_utelate_avklarte_og_beholde_en_linje_per_arbeidsgiver() {
        var agMangler = Arbeidsgiver.virksomhet("888888888");
        var agAvklart = Arbeidsgiver.virksomhet("777777777");

        var resultat = BrevGrunnlagArbeidsforholdInntektsmeldingTjeneste.mapInntektsmeldingStatus(
            List.of(status(AG, MOTTATT), status(agMangler, IKKE_MOTTAT), status(agAvklart, AVKLART_IKKE_PÅKREVD)));

        assertThat(resultat.arbeidsforholdInntektsmelding()).containsExactly(
            new ArbeidsforholdInntektsmeldingDto(AG.getIdentifikator(), true),
            new ArbeidsforholdInntektsmeldingDto(agMangler.getIdentifikator(), false));
    }

    @Test
    void skal_ikke_la_avklart_arbeidsforhold_påvirke_status_for_arbeidsgiver() {
        var resultat = BrevGrunnlagArbeidsforholdInntektsmeldingTjeneste.mapInntektsmeldingStatus(
            List.of(status(AG, MOTTATT), status(AG, AVKLART_IKKE_PÅKREVD)));

        assertThat(resultat.arbeidsforholdInntektsmelding()).containsExactly(new ArbeidsforholdInntektsmeldingDto(AG.getIdentifikator(), true));
    }

    private static ArbeidsforholdInntektsmeldingStatus status(Arbeidsgiver ag, ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus status) {
        return new ArbeidsforholdInntektsmeldingStatus(ag, InternArbeidsforholdRef.nyRef(), status);
    }
}
