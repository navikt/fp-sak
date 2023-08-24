package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class VurderATogFLiSammeOrganisasjonAndelDto {

    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;

    @NotNull
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer arbeidsinntekt;

    VurderATogFLiSammeOrganisasjonAndelDto() {
        // For Jackson
    }

    public VurderATogFLiSammeOrganisasjonAndelDto(Long andelsnr, Integer arbeidsinntekt) {
        this.andelsnr = andelsnr;
        this.arbeidsinntekt = arbeidsinntekt;
    }

    public Long getAndelsnr() {
        return andelsnr;
    }

    public Integer getArbeidsinntekt() {
        return arbeidsinntekt;
    }
}
