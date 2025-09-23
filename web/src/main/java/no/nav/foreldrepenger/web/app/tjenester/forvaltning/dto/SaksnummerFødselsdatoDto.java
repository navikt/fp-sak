package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.InputValideringRegexDato.DATO_PATTERN;
import static no.nav.vedtak.util.InputValideringRegex.FRITEKST;

import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class SaksnummerFødselsdatoDto implements AbacDto {

    @NotNull
    @Parameter(description = "Saksnummer")
    @QueryParam("saksnummer")
    @Digits(integer = 18, fraction = 0)
    private String saksnummer;

    @NotNull
    @Parameter(description = "Fødselsdato (YYYY-MM-DD)")
    @QueryParam("fødselsdato")
    @Pattern(regexp = DATO_PATTERN)
    private String fødselsdato;

    @Parameter(description = "Dødsdato (YYYY-MM-DD)")
    @QueryParam("dødsdato")
    @Pattern(regexp = DATO_PATTERN)
    private String dødsdato;

    @Parameter(description = "Begrunnelse, fx FAGSYSTEM-nr")
    @FormParam("begrunnelse")
    @Pattern(regexp = FRITEKST)
    private String begrunnelse;

    public SaksnummerFødselsdatoDto(@NotNull String saksnummer, @NotNull String fødselsdato, String dødsdato, String begrunnelse) {
        this.saksnummer = saksnummer;
        this.fødselsdato = fødselsdato;
        this.dødsdato = dødsdato;
        this.begrunnelse = begrunnelse;
    }

    public SaksnummerFødselsdatoDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.SAKSNUMMER, saksnummer);
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public LocalDate getFødselsdato() {
        return LocalDate.parse(fødselsdato);
    }

    public LocalDate getDødsdato() {
        return dødsdato != null ? LocalDate.parse(dødsdato) : null;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
