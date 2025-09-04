package no.nav.foreldrepenger.domene.rest.dto.fordeling;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.validering.ValidKodeverk;


public class FordelFastsatteVerdierDto {

    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer refusjonPrÅr;

    @Min(0)
    @Max(Integer.MAX_VALUE)
    @NotNull
    private Integer fastsattÅrsbeløpInklNaturalytelse;

    @NotNull
    @ValidKodeverk
    private Inntektskategori inntektskategori;


    FordelFastsatteVerdierDto() {
        // Jackson
    }

    public FordelFastsatteVerdierDto(@Min(0) @Max(Integer.MAX_VALUE) Integer refusjonPrÅr, @Min(0) @Max(Integer.MAX_VALUE) @NotNull Integer fastsattÅrsbeløpInklNaturalytelse, @NotNull Inntektskategori inntektskategori) {
        this.refusjonPrÅr = refusjonPrÅr;
        this.fastsattÅrsbeløpInklNaturalytelse = fastsattÅrsbeløpInklNaturalytelse;
        this.inntektskategori = inntektskategori;
    }

    public FordelFastsatteVerdierDto(@Min(0) @Max(Integer.MAX_VALUE) @NotNull Integer fastsattÅrsbeløpInklNaturalytelse, @NotNull Inntektskategori inntektskategori) {
        this.fastsattÅrsbeløpInklNaturalytelse = fastsattÅrsbeløpInklNaturalytelse;
        this.inntektskategori = inntektskategori;
    }

    public Integer getRefusjonPrÅr() {
        return refusjonPrÅr;
    }

    public void setRefusjonPrÅr(Integer refusjonPrÅr) {
        this.refusjonPrÅr = refusjonPrÅr;
    }

    public Integer getFastsattÅrsbeløpInklNaturalytelse() {
        return fastsattÅrsbeløpInklNaturalytelse;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

}
