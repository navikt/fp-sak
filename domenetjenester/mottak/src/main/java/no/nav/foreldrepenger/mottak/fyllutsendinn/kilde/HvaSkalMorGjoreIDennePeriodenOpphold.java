package no.nav.foreldrepenger.mottak.fyllutsendinn.kilde;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum HvaSkalMorGjoreIDennePeriodenOpphold {
    @JsonProperty("arbeid")
    ARBEID,
    @JsonProperty("utdanningPaHeltid")
    UTDANNING_PA_HELTID,
    @JsonProperty("arbeidOgUtdanningSomTilSammenBlirHeltid")
    ARBEID_OG_UTDANNING_SOM_TIL_SAMMEN_BLIR_HELTID,
    @JsonProperty("kvalifiseringsprogrammet")
    KVALIFISERINGSPROGRAMMET,
    @JsonProperty("introduksjonsprogrammet")
    INTRODUKSJONSPROGRAMMET,
    @JsonProperty("forSykTilATaSegAvBarnet")
    FOR_SYK_TIL_A_TA_SEG_AV_BARNET,
    @JsonProperty("innlagtPaHelseinstitusjon")
    INNLAGT_PA_HELSEINSTITUSJON;
}
