package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Pattern;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

import java.util.UUID;

public record AvklarPermisjonUtenSluttdatoDto(@NotNull @Pattern(regexp = InputValideringRegex.ARBEIDSGIVER) String arbeidsgiverIdent,
                                              UUID internArbeidsforholdId,
                                              @NotNull @ValidKodeverk BekreftetPermisjonStatus permisjonStatus) {}
