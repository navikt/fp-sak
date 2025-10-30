package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.Valid;

public class RefusjonskravForSentDto {

    @Valid
    private String arbeidsgiverIdent;

    @Valid
    private Boolean erRefusjonskravGyldig;

    RefusjonskravForSentDto() {
        // Jackson
    }

    public RefusjonskravForSentDto(@Valid String arbeidsgiverIdent, @Valid Boolean erRefusjonskravGyldig) {
        this.arbeidsgiverIdent = arbeidsgiverIdent;
        this.erRefusjonskravGyldig = erRefusjonskravGyldig;
    }

    public String getArbeidsgiverIdent() {
        return arbeidsgiverIdent;
    }

    public Boolean getErRefusjonskravGyldig() {
        return erRefusjonskravGyldig;
    }
}
