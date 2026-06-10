package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.QueryParam;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class ForvaltningOpprettForespørselDto implements AbacDto {

    @NotNull
    @QueryParam("behandlingUuid")
    @Valid
    @JsonProperty
    private UUID behandlingUuid;

    @NotNull
    @QueryParam("orgnummer")
    @Pattern(regexp = "^\\d{9}$", message = "Orgnummer må være 9 siffer")
    @JsonProperty
    private String orgnummer;

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public String getOrgnummer() {
        return orgnummer;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.BEHANDLING_UUID, getBehandlingUuid());
    }

    @Override
    public String toString() {
        return "behandlingUuid=" + behandlingUuid + ", orgnummer=***";
    }
}
