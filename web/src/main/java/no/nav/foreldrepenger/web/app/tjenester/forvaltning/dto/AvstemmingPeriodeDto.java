package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.InputValideringRegexDato.DATO_PATTERN;
import static no.nav.vedtak.util.InputValideringRegex.FRITEKST;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
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

    // Tidsrom mellom dager. Det er opp til 900 saker/dag - færre i helg/ferie. Tidsbruk 0,2-1s pr sak
    @NotNull
    @Parameter(description = "tidsrom for avstemming av 1 dag (sekunder)")
    @QueryParam("tidsrom")
    @Min(0)
    @Max(3600)
    private int tidsrom;

    @Parameter(description = "true gir saker med vedtak fattet i periode, false gir saker opprettet i periode")
    @QueryParam("vedtak")
    private boolean vedtak;

    public AvstemmingPeriodeDto(@NotNull String key, @NotNull String fom, @NotNull String tom, int tidsrom, boolean vedtak) {
        this.key = key;
        this.fom = fom;
        this.tom = tom;
        this.tidsrom = tidsrom;
        this.vedtak = vedtak;
    }

    public AvstemmingPeriodeDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return TilbakeRestTjeneste.opprett();
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

    public boolean isVedtak() {
        return vedtak;
    }
}
