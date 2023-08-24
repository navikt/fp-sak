package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.constraints.NotNull;

public class VurderNyoppstartetFLDto {

    @NotNull
    private Boolean erNyoppstartetFL;

    VurderNyoppstartetFLDto() {
        // For Jackson
    }

    public VurderNyoppstartetFLDto(Boolean erNyoppstartetFL) {
        this.erNyoppstartetFL = erNyoppstartetFL;
    }

    public void setErNyoppstartetFL(Boolean erNyoppstartetFL) {
        this.erNyoppstartetFL = erNyoppstartetFL;
    }

    public Boolean erErNyoppstartetFL() {
        return erNyoppstartetFL;
    }
}
