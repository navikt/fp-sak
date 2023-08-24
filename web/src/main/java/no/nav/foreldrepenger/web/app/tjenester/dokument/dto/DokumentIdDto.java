package no.nav.foreldrepenger.web.app.tjenester.dokument.dto;

import jakarta.validation.constraints.Digits;

public class DokumentIdDto {
    @Digits(integer = 18, fraction = 0)
    private String dokumentId;

    public DokumentIdDto(String dokumentId) {
        this.dokumentId = dokumentId;
    }

    public String getDokumentId() {
        return dokumentId;
    }


}
