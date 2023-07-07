package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class FastsettMånedsinntektFLDto {

    @NotNull
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer maanedsinntekt;

    FastsettMånedsinntektFLDto() {
        // For Jackson
    }

    public FastsettMånedsinntektFLDto(Integer maanedsInntekt) {
        this.maanedsinntekt = maanedsInntekt;
    }

    public void setMaanedsinntekt(Integer maanedsinntekt) {
        this.maanedsinntekt = maanedsinntekt;
    }

    public Integer getMaanedsinntekt() {
        return maanedsinntekt;
    }
}
