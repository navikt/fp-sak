package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto;

import javax.validation.constraints.NotNull;

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
