package no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RelasjonsRolleTypeDto {

    EKTE("EKTE"),
    BARN("BARN"),
    FARA("FARA"),
    MORA("MORA"),
    REGISTRERT_PARTNER("REPA"),
    MEDMOR("MMOR"),
    ANNEN_PART_FRA_SØKNAD("ANPA")
    ;

    @JsonValue
    private String kode;

    RelasjonsRolleTypeDto(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
