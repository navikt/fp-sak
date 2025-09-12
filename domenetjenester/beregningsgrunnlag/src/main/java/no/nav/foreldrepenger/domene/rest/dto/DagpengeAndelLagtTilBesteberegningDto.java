package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;

public class DagpengeAndelLagtTilBesteberegningDto {

    @Valid
    @NotNull
    private FastsatteVerdierForBesteberegningDto fastsatteVerdier;

    public DagpengeAndelLagtTilBesteberegningDto() {
        // For Jackson
    }

    public DagpengeAndelLagtTilBesteberegningDto(int fastsattBeløp, Inntektskategori inntektskategori) {
        this.fastsatteVerdier = new FastsatteVerdierForBesteberegningDto(fastsattBeløp, inntektskategori);
    }

    public FastsatteVerdierForBesteberegningDto getFastsatteVerdier() {
        return fastsatteVerdier;
    }

}
