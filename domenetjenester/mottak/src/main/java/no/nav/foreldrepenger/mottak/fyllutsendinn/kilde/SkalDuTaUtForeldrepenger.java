package no.nav.foreldrepenger.mottak.fyllutsendinn.kilde;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SkalDuTaUtForeldrepenger {
    @JsonProperty("ja")
    JA,
    @JsonProperty("neiJegSkalHaOppholdIForeldrepengeneMine")
    NEI_JEG_SKAL_HA_OPPHOLD_I_FORELDREPENGENE_MINE;
}
