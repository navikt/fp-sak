package no.nav.foreldrepenger.domene.rest.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.domene.modell.Inntektskategori;
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

    FastsettMånedsinntektUtenInntektsmeldingAndelDto() { //NOSONAR
        // For Jackson
    }

    public FastsettMånedsinntektUtenInntektsmeldingAndelDto(Long andelsnr, FastsatteVerdierDto fastsatteVerdier) { // NOSONAR
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
