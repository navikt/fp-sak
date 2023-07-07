package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.constraints.NotNull;

public class FastsettEtterlønnSluttpakkeDto {

    @NotNull
    private Integer fastsattPrMnd;

    FastsettEtterlønnSluttpakkeDto() {
        // For Jackson
    }

    public FastsettEtterlønnSluttpakkeDto(Integer fastsattPrMnd) {
        this.fastsattPrMnd = fastsattPrMnd;
    }

    public Integer getFastsattPrMnd() {
        return fastsattPrMnd;
    }

    public void setFastsattPrMnd(Integer fastsattPrMnd) {
        this.fastsattPrMnd = fastsattPrMnd;
    }
}
