package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto;

import javax.validation.constraints.NotNull;

public class FastsettEtterlønnSluttpakkeDto {

    @NotNull
    private Integer fastsattPrMnd;

    FastsettEtterlønnSluttpakkeDto() {
        // For Jackson
    }

    public FastsettEtterlønnSluttpakkeDto(Integer fastsattPrMnd) { // NOSONAR
        this.fastsattPrMnd = fastsattPrMnd;
    }

    public Integer getFastsattPrMnd() {
        return fastsattPrMnd;
    }

    public void setFastsattPrMnd(Integer fastsattPrMnd) {
        this.fastsattPrMnd = fastsattPrMnd;
    }
}
