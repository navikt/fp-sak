package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SvpTilretteleggingTypeDto {

    @JsonProperty("HEL_TILRETTELEGGING") HEL_TILRETTELEGGING,
    @JsonProperty("DELVIS_TILRETTELEGGING") DELVIS_TILRETTELEGGING,
    @JsonProperty("INGEN_TILRETTELEGGING") INGEN_TILRETTELEGGING

}
