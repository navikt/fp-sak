package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public class FastsettMånedsinntektUtenInntektsmeldingAndelDto {

    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;

    @Min(0)
    @NotNull
    @Max(Integer.MAX_VALUE)
    private Integer fastsattBeløp;

    @ValidKodeverk
    private Inntektskategori inntektskategori;

    FastsettMånedsinntektUtenInntektsmeldingAndelDto() {
        // For Jackson
    }

    public FastsettMånedsinntektUtenInntektsmeldingAndelDto(Long andelsnr, FastsatteVerdierDto fastsatteVerdier) {
        this.andelsnr = andelsnr;
        this.fastsattBeløp = fastsatteVerdier.getFastsattBeløp();
        this.inntektskategori = fastsatteVerdier.getInntektskategori();
    }

    public Long getAndelsnr() {
        return andelsnr;
    }


    public Integer getFastsattBeløp() {
        return fastsattBeløp;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

}
