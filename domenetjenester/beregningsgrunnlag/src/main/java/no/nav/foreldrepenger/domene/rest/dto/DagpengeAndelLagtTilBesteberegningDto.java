package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class DagpengeAndelLagtTilBesteberegningDto {

    @Valid
    @NotNull
    private FastsatteVerdierForBesteberegningDto fastsatteVerdier;

    public DagpengeAndelLagtTilBesteberegningDto() {
        // For Jackson
    }

    public DagpengeAndelLagtTilBesteberegningDto(@NotNull FastsatteVerdierForBesteberegningDto fastsatteVerdier) {
        this.fastsatteVerdier = fastsatteVerdier;
    }

    public FastsatteVerdierForBesteberegningDto getFastsatteVerdier() {
        return fastsatteVerdier;
    }

}
