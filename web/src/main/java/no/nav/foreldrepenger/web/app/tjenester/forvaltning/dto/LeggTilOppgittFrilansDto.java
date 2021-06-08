package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.QueryParam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class LeggTilOppgittFrilansDto implements AbacDto {

    private static final String DATO_PATTERN = "(\\d{4}-\\d{2}-\\d{2})";

    @NotNull
    @QueryParam("behandlingId")
    @Pattern(regexp = "^[a-fA-F0-9-]+$")
    @JsonProperty
    private String behandlingId;

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

    public LeggTilOppgittFrilansDto(@NotNull String behandlingId, @NotNull String frilansFom, String frilansTom, @NotNull String stpOpptjening) {
        this.behandlingId = behandlingId;
        this.frilansFom = frilansFom;
        this.frilansTom = frilansTom;
        this.stpOpptjening = stpOpptjening;
    }

    public LeggTilOppgittFrilansDto() {
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

    @JsonIgnore
    public Long getBehandlingId() {
        return behandlingId != null && getBehandlingUUID() == null ? Long.valueOf(behandlingId) : null;
    }

    @JsonIgnore
    public UUID getBehandlingUUID() {
        return behandlingId != null && behandlingId.contains("-") ? UUID.fromString(behandlingId) : null;
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
