package no.nav.foreldrepenger.domene.rest.dto;

import javax.validation.constraints.NotNull;

public class VurderEtterlønnSluttpakkeDto {

    @NotNull
    private Boolean erEtterlønnSluttpakke;

    VurderEtterlønnSluttpakkeDto() {
        // For Jackson
    }

    public VurderEtterlønnSluttpakkeDto(Boolean erEtterlønnSluttpakke) { // NOSONAR
        this.erEtterlønnSluttpakke = erEtterlønnSluttpakke;
    }

    public Boolean erEtterlønnSluttpakke() {
        return erEtterlønnSluttpakke;
    }

    public void setErEtterlønnSluttpakke(Boolean erEtterlønnSluttpakke) {
        this.erEtterlønnSluttpakke = erEtterlønnSluttpakke;
    }
}
