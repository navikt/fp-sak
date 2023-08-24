package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class FastsatteAndelerTidsbegrensetDto {

    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Integer bruttoFastsattInntekt;

    FastsatteAndelerTidsbegrensetDto() {
        // Jackson
    }

    public FastsatteAndelerTidsbegrensetDto(Long andelsnr,
                                            Integer bruttoFastsattInntekt) {
        this.andelsnr = andelsnr;
        this.bruttoFastsattInntekt = bruttoFastsattInntekt;
    }
    public Long getAndelsnr() { return andelsnr; }

    public Integer getBruttoFastsattInntekt() {
        return bruttoFastsattInntekt;
    }

}
