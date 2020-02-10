package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto;

import javax.validation.constraints.NotNull;

public class VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto {

    @NotNull
    private Boolean erNyIArbeidslivet;

    VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto() {
        // For Jackson
    }

    public VurderSelvstendigNæringsdrivendeNyIArbeidslivetDto(Boolean erNyIArbeidslivet) { // NOSONAR
        this.erNyIArbeidslivet = erNyIArbeidslivet;
    }

    public void setErNyIArbeidslivet(Boolean erNyIArbeidslivet) {
        this.erNyIArbeidslivet = erNyIArbeidslivet;
    }

    public Boolean erNyIArbeidslivet() {
        return erNyIArbeidslivet;
    }
}
