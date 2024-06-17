package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum UforeTypeCode {
    @JsonEnumDefaultValue UKJENT,
    /**
     * Uføre
     */
    UFORE,
    /**
     * Uføre m/yrkesskade
     */
    UF_M_YRKE,
    /**
     * Første virkningsdato, ikke ufør
     */
    VIRK_IKKE_UFOR,
    /**
     * Yrkesskade
     */
    YRKE;
}
