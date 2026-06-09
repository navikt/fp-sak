package no.nav.foreldrepenger.mottak.fyllutsendinn.kilde;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum HvilkenTypeVirksomhetDriverDu {
    @JsonProperty("fiske")
    FISKE,
    @JsonProperty("jordbruk")
    JORDBRUK,
    @JsonProperty("dagmammaEllerFamiliebarnehageIEgetHjem")
    DAGMAMMA_ELLER_FAMILIEBARNEHAGE_I_EGET_HJEM,
    @JsonProperty("annenTypeVirksomhet")
    ANNEN_TYPE_VIRKSOMHET;
}
