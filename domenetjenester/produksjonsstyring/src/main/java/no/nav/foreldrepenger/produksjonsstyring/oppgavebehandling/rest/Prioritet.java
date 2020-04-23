package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.rest;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Prioritet {
    HOY,
    @JsonEnumDefaultValue
    NORM,
    LAV
}
