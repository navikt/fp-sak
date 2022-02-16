package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;

public record AvklarPermisjonUtenSluttdatoDto(String arbeidsgiverIdent, String internArbeidsforholdId, @NotNull BekreftetPermisjonStatus permisjonStatus) {}
