package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto;

import javax.validation.constraints.NotNull;

public class VurderLønnsendringDto {

    @NotNull
    private Boolean erLønnsendringIBeregningsperioden;

    VurderLønnsendringDto() {
        // For Jackson
    }

    public VurderLønnsendringDto(Boolean erLønnsendringIBeregningsperioden) { // NOSONAR
        this.erLønnsendringIBeregningsperioden = erLønnsendringIBeregningsperioden;
    }

    public Boolean erLønnsendringIBeregningsperioden() {
        return erLønnsendringIBeregningsperioden;
    }

    public void setErLønnsendringIBeregningsperioden(Boolean erLønnsendringIBeregningsperioden) {
        this.erLønnsendringIBeregningsperioden = erLønnsendringIBeregningsperioden;
    }
}
