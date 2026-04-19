package no.nav.foreldrepenger.mottak.fyllutsendinn;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Shared enum for boolean-as-string fields ("ja" / "nei"). */
public enum JaNei {
    @JsonProperty("ja")  JA,
    @JsonProperty("nei") NEI,
    @JsonEnumDefaultValue UDEFINERT
}
