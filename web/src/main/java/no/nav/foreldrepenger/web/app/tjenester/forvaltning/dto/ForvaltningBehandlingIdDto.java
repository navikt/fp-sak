package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

/**
 * For at Swagger-brukerne skal slippe å bli forvirret av at det også kommer opp saksnummer og UUID,
 * har vi her en forenklet versjon av BehandlingIdDto til forvaltningstjenestene.
 */
public class ForvaltningBehandlingIdDto implements AbacDto {


    //TODO palfi trenger ikke egen for forvaltning lenger
    @NotNull
    @QueryParam("behandlingUuid")
    @Valid
    @JsonProperty
    private UUID behandlingUuid;

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.BEHANDLING_UUID, getBehandlingUuid());
    }

    @Override
    public String toString() {
        return behandlingUuid.toString();
    }
}
