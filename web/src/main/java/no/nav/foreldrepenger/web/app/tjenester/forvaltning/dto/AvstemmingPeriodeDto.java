package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class AvstemmingPeriodeDto implements AbacDto {

    private static final String DATO_PATTERN = "(\\d{4}-\\d{2}-\\d{2})";

    @NotNull
    @Parameter(description = "key (secret)")
    @QueryParam("key")
    private String key;

    @NotNull
    @Parameter(description = "fom (YYYY-MM-DD)")
    @QueryParam("fom")
    @Pattern(regexp = DATO_PATTERN)
    private String fom;

    @NotNull
    @Parameter(description = "tom (YYYY-MM-DD)")
    @QueryParam("tom")
    @Pattern(regexp = DATO_PATTERN)
    private String tom;

    public AvstemmingPeriodeDto(@NotNull String key, @NotNull String fom, @NotNull String tom) {
        this.key = key;
        this.fom = fom;
        this.tom = tom;
    }

    public AvstemmingPeriodeDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett();
    }

    public String getKey() {
        return key;
    }

    public LocalDate getFom() {
        return LocalDate.parse(fom, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public LocalDate getTom() {
        return LocalDate.parse(tom, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
