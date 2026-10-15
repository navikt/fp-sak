package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

public record ArbeidsforholdInntektsmeldingStatus(@NotNull Arbeidsgiver arbeidsgiver, @NotNull InntektsmeldingStatus inntektsmeldingStatus){
    @Override
    public String toString() {
        return "ArbeidsforholdInntektsmeldingStatus{" + "arbeidsgiver=" + arbeidsgiver + ", status=" + inntektsmeldingStatus + '}';
    }
    public enum InntektsmeldingStatus {
        MOTTATT,
        IKKE_MOTTAT,
        AVKLART_IKKE_PÅKREVD,
    }

}
