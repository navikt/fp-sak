package no.nav.foreldrepenger.domene.rest.dto;

import java.math.BigDecimal;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import no.nav.foreldrepenger.domene.modell.Inntektskategori;


public class FastsatteVerdierDto {

    private static final int MÅNEDER_I_1_ÅR = 12;

    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer refusjon;

    private Integer refusjonPrÅr;

    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer fastsattBeløp;

    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer fastsattÅrsbeløp;

    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer fastsattÅrsbeløpInklNaturalytelse;

    private Inntektskategori inntektskategori;

    private Boolean skalHaBesteberegning;

    FastsatteVerdierDto() { // NOSONAR
        // Jackson
    }

    public FastsatteVerdierDto(Integer refusjon,
                               Integer fastsattBeløp,
                               Inntektskategori inntektskategori,
                               Boolean skalHaBesteberegning) {
        this.refusjon = refusjon;
        this.refusjonPrÅr = refusjon == null ? null : refusjon*MÅNEDER_I_1_ÅR;
        this.fastsattBeløp = fastsattBeløp;
        this.inntektskategori = inntektskategori;
        this.skalHaBesteberegning = skalHaBesteberegning;
    }


    public FastsatteVerdierDto(Integer fastsattÅrsbeløp,
                               Inntektskategori inntektskategori,
                               Boolean skalHaBesteberegning) {
        this.fastsattBeløp = fastsattÅrsbeløp / MÅNEDER_I_1_ÅR;
        this.fastsattÅrsbeløp = fastsattÅrsbeløp;
        this.inntektskategori = inntektskategori;
        this.skalHaBesteberegning = skalHaBesteberegning;
    }

    public FastsatteVerdierDto(Integer fastsattBeløp, Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
        this.fastsattBeløp = fastsattBeløp;
        this.fastsattÅrsbeløp = fastsattBeløp * MÅNEDER_I_1_ÅR;
    }


    public FastsatteVerdierDto(Integer fastsattBeløp) {
        this.fastsattBeløp = fastsattBeløp;
        this.fastsattÅrsbeløp = fastsattBeløp * MÅNEDER_I_1_ÅR;
    }

    public Integer getRefusjon() {
        return refusjon;
    }

    public Integer getRefusjonPrÅr() {
        if (refusjonPrÅr != null) {
            return refusjonPrÅr;
        }
        return refusjon == null ? null : refusjon * MÅNEDER_I_1_ÅR;
    }

    public void setRefusjonPrÅr(Integer refusjonPrÅr) {
        this.refusjonPrÅr = refusjonPrÅr;
    }

    public Integer getFastsattBeløp() {
        return fastsattBeløp;
    }

    public Integer getFastsattÅrsbeløp() {
        return fastsattÅrsbeløp;
    }

    public Integer getFastsattÅrsbeløpInklNaturalytelse() {
        return fastsattÅrsbeløpInklNaturalytelse;
    }

    public BigDecimal finnEllerUtregnFastsattBeløpPrÅr() {
        if (fastsattÅrsbeløpInklNaturalytelse != null) {
            return BigDecimal.valueOf(fastsattÅrsbeløpInklNaturalytelse);
        }
        if (fastsattÅrsbeløp != null) {
            return BigDecimal.valueOf(fastsattÅrsbeløp);
        }
        if (fastsattBeløp == null) {
            throw new IllegalStateException("Feil under oppdatering: Hverken årslønn eller månedslønn er satt.");
        }
        return BigDecimal.valueOf((long) fastsattBeløp * MÅNEDER_I_1_ÅR);
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public Boolean getSkalHaBesteberegning() {
        return skalHaBesteberegning;
    }
}
