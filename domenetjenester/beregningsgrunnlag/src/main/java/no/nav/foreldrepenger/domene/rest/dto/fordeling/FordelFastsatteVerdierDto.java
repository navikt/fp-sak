package no.nav.foreldrepenger.domene.rest.dto.fordeling;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori;
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


    FordelFastsatteVerdierDto() { // NOSONAR
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
