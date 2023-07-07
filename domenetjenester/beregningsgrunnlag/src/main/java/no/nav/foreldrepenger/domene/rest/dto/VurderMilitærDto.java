package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.constraints.NotNull;

public class VurderMilitærDto {

    @NotNull
    private Boolean harMilitaer;

    VurderMilitærDto() {
        // For Jackson
    }

    public VurderMilitærDto(Boolean harMilitaer) {
        this.harMilitaer = harMilitaer;
    }


    public Boolean getHarMilitaer() {
        return harMilitaer;
    }

    public void setHarMilitaer(Boolean harMilitaer) {
        this.harMilitaer = harMilitaer;
    }
}
