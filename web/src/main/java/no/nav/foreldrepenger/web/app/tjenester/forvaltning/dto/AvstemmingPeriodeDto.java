package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.QueryParam;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.InputValideringRegexDato.DATO_PATTERN;
import static no.nav.vedtak.util.InputValideringRegex.FRITEKST;

public class AvstemmingPeriodeDto implements AbacDto {

    @NotNull
    @Parameter(description = "key (secret)")
    @FormParam("key")
    @Pattern(regexp = FRITEKST)
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
