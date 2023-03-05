package no.nav.foreldrepenger.domene.rest.dto;

import javax.validation.constraints.NotNull;

public class VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto {

    @NotNull
    private Boolean erNyIArbeidslivet;

    VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto() {
        // For Jackson
    }

    public VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto(Boolean erNyIArbeidslivet) {
        this.erNyIArbeidslivet = erNyIArbeidslivet;
    }

    public void setErNyIArbeidslivet(Boolean erNyIArbeidslivet) {
        this.erNyIArbeidslivet = erNyIArbeidslivet;
    }

    public Boolean erNyIArbeidslivet() {
        return erNyIArbeidslivet;
    }
}
