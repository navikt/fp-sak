package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;

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

    public BesteberegningFødendeKvinneAndelDto(Long andelsnr, Integer inntektPrMnd, Inntektskategori inntektskategori,
                                               boolean lagtTilAvSaksbehandler) {
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
        this.andelsnr = andelsnr;
        fastsatteVerdier = new FastsatteVerdierForBesteberegningDto(inntektPrMnd, inntektskategori);
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
