package no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk;

import com.fasterxml.jackson.annotation.JsonValue;

public enum FagsakYtelseTypeDto {

    ENGANGSTØNAD("ES"),
    FORELDREPENGER("FP"),
    SVANGERSKAPSPENGER("SVP")
    ;

    @JsonValue
    private final String kode;

    FagsakYtelseTypeDto(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
