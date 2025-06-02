package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.constraints.NotNull;

public class FastsettInntektForArbeidUnderAAPDto {

    @NotNull
    private Integer fastsattPrMnd;

    FastsettInntektForArbeidUnderAAPDto() {
        // For Jackson
    }

    public FastsettInntektForArbeidUnderAAPDto(Integer fastsattPrMnd) {
        this.fastsattPrMnd = fastsattPrMnd;
    }

    public Integer getFastsattPrMnd() {
        return fastsattPrMnd;
    }

    public void setFastsattPrMnd(Integer fastsattPrMnd) {
        this.fastsattPrMnd = fastsattPrMnd;
    }
}
