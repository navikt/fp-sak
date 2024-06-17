package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public record ArbeidsforholdInntektsmeldingStatus(@NotNull Arbeidsgiver arbeidsgiver, @NotNull InternArbeidsforholdRef ref,
                                                  @NotNull InntektsmeldingStatus inntektsmeldingStatus) {
    @Override
    public String toString() {
        return "ArbeidsforholdMangel{" + "arbeidsgiver=" + arbeidsgiver + ", ref=" + ref + ", status=" + inntektsmeldingStatus + '}';
    }

    public enum InntektsmeldingStatus {
        MOTTATT,
        IKKE_MOTTAT,
        AVKLART_IKKE_PÅKREVD,
    }

}
