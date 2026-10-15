package no.nav.foreldrepenger.domene.fpinntektsmelding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OrganisasjonsnummerMedStatusDto(@NotNull @Valid OrganisasjonsnummerDto orgnummer, boolean erInntektsmeldingMottatt) {}

