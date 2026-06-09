package no.nav.foreldrepenger.mottak.fyllutsendinn.kilde;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum HvorHarDuBoddDeSiste12Manedene {
    @JsonProperty("kunBoddINorge")
    KUN_BODD_I_NORGE,
    @JsonProperty("boddIUtlandetHeltEllerDelvis")
    BODD_I_UTLANDET_HELT_ELLER_DELVIS;
}
