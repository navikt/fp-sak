package no.nav.foreldrepenger.domene.rest.dto;

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
