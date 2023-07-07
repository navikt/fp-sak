package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.constraints.NotNull;

public class ArbeidstakerandelUtenIMMottarYtelseDto {

    @NotNull
    private long andelsnr;
    private Boolean mottarYtelse;

    public ArbeidstakerandelUtenIMMottarYtelseDto() {
        // For jackson
    }

    public ArbeidstakerandelUtenIMMottarYtelseDto(long andelsnr, Boolean mottarYtelse) {
        this.andelsnr = andelsnr;
        this.mottarYtelse = mottarYtelse;
    }

    public long getAndelsnr() {
        return andelsnr;
    }

    public Boolean getMottarYtelse() {
        return mottarYtelse;
    }
}
