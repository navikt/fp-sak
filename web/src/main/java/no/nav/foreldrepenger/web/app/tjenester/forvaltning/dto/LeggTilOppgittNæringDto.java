package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.util.InputValideringRegex;

public class LeggTilOppgittNæringDto implements AbacDto {

    private static final String DATO_PATTERN = "(\\d{4}-\\d{2}-\\d{2})";

    @NotNull
    @QueryParam("behandlingUuid")
    @Valid
    @JsonProperty
    private UUID behandlingUuid;

    @NotNull
    @Parameter(description = "type A/D/F/J")
    @QueryParam("typeKode")
    @DefaultValue("A")
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String typeKode;

    @NotNull
    @Parameter(description = "YYYY-MM-DD")
    @QueryParam("fom")
    @Pattern(regexp = DATO_PATTERN)
    private String fom;

    @Parameter(description = "YYYY-MM-DD")
    @QueryParam("tom")
    @Pattern(regexp = DATO_PATTERN)
    private String tom;

    @QueryParam("orgnummer")
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String orgnummer;

    @QueryParam("regnskapNavn")
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String regnskapNavn;

    @QueryParam("regnskapTlf")
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String regnskapTlf;

    @Parameter(description = "nyoppstartet J/N")
    @QueryParam("nyoppstartet")
    @DefaultValue("N")
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String nyoppstartet;

    @Parameter(description = "varig endring J/N")
    @QueryParam("varigEndring")
    @DefaultValue("N")
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String varigEndring;

    @Parameter(description = "YYYY-MM-DD")
    @QueryParam("endringsDato")
    @Pattern(regexp = DATO_PATTERN)
    private String endringsDato;

    @QueryParam("begrunnelse")
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    @QueryParam("bruttoBeløp")
    @DefaultValue("0")
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String bruttoBeløp;

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

    public String getTypeKode() {
        return typeKode;
    }

    public LocalDate getFom() {
        return getLocalDate(fom);
    }

    public LocalDate getTom() {
        return getLocalDate(tom);
    }

    public String getOrgnummer() {
        return orgnummer;
    }

    public String getRegnskapNavn() {
        return regnskapNavn;
    }

    public String getRegnskapTlf() {
        return regnskapTlf;
    }

    public String getNyoppstartet() {
        return nyoppstartet;
    }

    public String getVarigEndring() {
        return varigEndring;
    }

    public LocalDate getEndringsDato() {
        return getLocalDate(endringsDato);
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getBruttoBeløp() {
        return bruttoBeløp;
    }

    private LocalDate getLocalDate(String datoString) {
        if (datoString != null) {
            return LocalDate.parse(datoString, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return null;
    }
}
