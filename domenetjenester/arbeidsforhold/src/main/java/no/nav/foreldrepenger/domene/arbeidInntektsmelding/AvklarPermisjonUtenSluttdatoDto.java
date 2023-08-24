package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;

public record AvklarPermisjonUtenSluttdatoDto(@NotNull String arbeidsgiverIdent, String internArbeidsforholdId, @NotNull BekreftetPermisjonStatus permisjonStatus) {}
