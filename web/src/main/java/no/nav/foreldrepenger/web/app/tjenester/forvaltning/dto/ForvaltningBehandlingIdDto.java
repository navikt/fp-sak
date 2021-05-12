package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.QueryParam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

/**
 * For at Swagger-brukerne skal slippe å bli forvirret av at det også kommer opp saksnummer og UUID,
 * har vi her en forenklet versjon av BehandlingIdDto til forvaltningstjenestene.
 */
public class ForvaltningBehandlingIdDto implements AbacDto {

    //TODO palfi bare UUID
    @NotNull
    @QueryParam("behandlingId")
    @Pattern(regexp = "^[a-fA-F0-9-]+$")
    @JsonProperty
    private String behandlingId;

    @JsonIgnore
    public Long getBehandlingId() {
        return behandlingId != null && getBehandlingUUID() == null ? Long.valueOf(behandlingId) : null;
    }

    @JsonIgnore
    public UUID getBehandlingUUID() {
        return behandlingId != null && behandlingId.contains("-") ? UUID.fromString(behandlingId) : null;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        var abac = AbacDataAttributter.opprett();
        if (getBehandlingId() != null) {
            abac.leggTil(AppAbacAttributtType.BEHANDLING_ID, getBehandlingId());
        }
        if (getBehandlingUUID() != null) {
            abac.leggTil(AppAbacAttributtType.BEHANDLING_UUID, getBehandlingUUID());
        }
        return abac;
    }

    @Override
    public String toString() {
        return behandlingId;
    }
}
