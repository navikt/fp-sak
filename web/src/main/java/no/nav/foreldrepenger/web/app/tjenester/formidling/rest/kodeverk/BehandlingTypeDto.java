package no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BehandlingTypeDto {

    FØRSTEGANGSSØKNAD("BT-002"),
    KLAGE("BT-003"),
    REVURDERING("BT-004"),
    ANKE("BT-008"),
    INNSYN("BT-006"),

    /** Tilbakekrevingene brukes mot personoversikt inntil videre. Kan vurdere ae0041 klage/tilbake */
    TILBAKEKREVING_ORDINÆR("BT-007"),
    TILBAKEKREVING_REVURDERING("BT-009")
    ;

    @JsonValue
    private final String kode;

    BehandlingTypeDto(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }
}
