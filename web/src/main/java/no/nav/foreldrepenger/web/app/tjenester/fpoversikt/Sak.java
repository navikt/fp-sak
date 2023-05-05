package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FpSak.class, name = "foreldrepenger"),
    @JsonSubTypes.Type(value = SvpSak.class, name = "svangerskapspenger"),
    @JsonSubTypes.Type(value = EsSak.class, name = "engangsstønad"),
})
public interface Sak {
}
