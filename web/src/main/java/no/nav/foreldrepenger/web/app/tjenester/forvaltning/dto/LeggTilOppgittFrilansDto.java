package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.InputValideringRegexDato.DATO_PATTERN;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.QueryParam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class LeggTilOppgittFrilansDto implements AbacDto {

    @NotNull
    @QueryParam("behandlingUuid")
    @Valid
    @JsonProperty
    private UUID behandlingUuid;

    @NotNull
    @Parameter(description = "YYYY-MM-DD")
    @QueryParam("frilansFom")
    @Pattern(regexp = DATO_PATTERN)
    private String frilansFom;

    @Parameter(description = "YYYY-MM-DD")
    @QueryParam("frilansTom")
    @Pattern(regexp = DATO_PATTERN)
    private String frilansTom;

    @NotNull
    @Parameter(description = "YYYY-MM-DD")
    @QueryParam("stpOpptjening")
    @Pattern(regexp = DATO_PATTERN)
    private String stpOpptjening;

    @Override
    public AbacDataAttributter abacAttributter() {
        var abac = AbacDataAttributter.opprett();
        if (getBehandlingUuid() != null) {
            abac.leggTil(AppAbacAttributtType.BEHANDLING_UUID, getBehandlingUuid());
        }
        return abac;
    }

    @JsonIgnore
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public LocalDate getFrilansFom() {
        return getLocalDate(frilansFom);
    }

    public LocalDate getFrilansTom() {
        return getLocalDate(frilansTom);
    }

    public LocalDate getStpOpptjening() {
        return getLocalDate(stpOpptjening);
    }

    private LocalDate getLocalDate(String datoString) {
        if (datoString != null) {
            return LocalDate.parse(datoString, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return null;
    }
}
