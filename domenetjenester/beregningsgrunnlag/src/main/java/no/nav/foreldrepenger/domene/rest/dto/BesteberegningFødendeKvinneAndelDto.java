package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class BesteberegningFødendeKvinneAndelDto {


    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;

    private Boolean nyAndel;

    @NotNull
    private Boolean lagtTilAvSaksbehandler;

    @Valid
    @NotNull
    private FastsatteVerdierForBesteberegningDto fastsatteVerdier;


    BesteberegningFødendeKvinneAndelDto() {
        // For Jackson
    }

    public BesteberegningFødendeKvinneAndelDto(Long andelsnr, @NotNull FastsatteVerdierForBesteberegningDto fastsatteVerdier,
                                               @NotNull Boolean lagtTilAvSaksbehandler) {
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
        this.andelsnr = andelsnr;
        this.fastsatteVerdier = fastsatteVerdier;
    }

    public FastsatteVerdierForBesteberegningDto getFastsatteVerdier() {
        return fastsatteVerdier;
    }

    public Long getAndelsnr() {
        return andelsnr;
    }

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public Boolean getNyAndel() {
        return nyAndel;
    }

}
