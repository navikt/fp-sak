package no.nav.foreldrepenger.domene.rest.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public class FastsatteVerdierForBesteberegningDto {

    private static final int MÅNEDER_I_1_ÅR = 12;

    @NotNull
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer fastsattBeløp;

    @NotNull
    @ValidKodeverk
    private Inntektskategori inntektskategori;

    FastsatteVerdierForBesteberegningDto() {
        // Jackson
    }

    public FastsatteVerdierForBesteberegningDto(Integer fastsattBeløp,
                                                Inntektskategori inntektskategori) {
        this.fastsattBeløp = fastsattBeløp;
        this.inntektskategori = inntektskategori;
    }

    public Integer getFastsattBeløp() {
        return fastsattBeløp;
    }

    public BigDecimal finnFastsattBeløpPrÅr() {
        if (fastsattBeløp == null) {
            throw new IllegalStateException("Feil under oppdatering: Månedslønn er ikke satt");
        }
        return BigDecimal.valueOf((long) fastsattBeløp * MÅNEDER_I_1_ÅR);
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public Boolean getSkalHaBesteberegning() {
        return true;
    }

}
