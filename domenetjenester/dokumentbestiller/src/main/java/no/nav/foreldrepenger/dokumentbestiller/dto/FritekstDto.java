package no.nav.foreldrepenger.dokumentbestiller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonValue;

public record FritekstDto(@Valid @Size(max = 10_000) @JsonValue String verdi) {

}
