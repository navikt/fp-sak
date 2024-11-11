package no.nav.foreldrepenger.domene.fpinntektsmelding;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonValue;

public record SaksnummerDto(@NotNull @JsonValue String saksnr) {
}
