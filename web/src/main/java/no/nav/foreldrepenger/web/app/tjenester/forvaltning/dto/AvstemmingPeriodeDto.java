package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.InputValideringRegexDato.DATO_PATTERN;
import static no.nav.vedtak.util.InputValideringRegex.FRITEKST;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

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

    @NotNull
    @Parameter(description = "tidsrom for kjøring (sekunder)")
    @QueryParam("tidsrom")
    private int tidsrom;

    public AvstemmingPeriodeDto(@NotNull String key, @NotNull String fom, @NotNull String tom, int tidsrom) {
        this.key = key;
        this.fom = fom;
        this.tom = tom;
        this.tidsrom = tidsrom;
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

    public int getTidsrom() {
        return tidsrom;
    }
}
