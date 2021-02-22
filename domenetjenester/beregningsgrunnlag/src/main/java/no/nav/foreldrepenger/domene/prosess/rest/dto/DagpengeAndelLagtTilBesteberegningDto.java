package no.nav.foreldrepenger.domene.prosess.rest.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
