package no.nav.foreldrepenger.mottak.fyllutsendinn.kilde;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum HvorSkalDuBoDeNeste12Manedene {
    @JsonProperty("kunBoINorge")
    KUN_BO_I_NORGE,
    @JsonProperty("boIUtlandetHeltEllerDelvis")
    BO_I_UTLANDET_HELT_ELLER_DELVIS;
}
