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
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class SaksnummerTermindatoDto implements AbacDto {

    @NotNull
    @Parameter(description = "Saksnummer")
    @QueryParam("saksnummer")
    @Digits(integer = 18, fraction = 0)
    private String saksnummer;

    @NotNull
    @Parameter(description = "Termindato (YYYY-MM-DD)")
    @QueryParam("termindato")
    @Pattern(regexp = DATO_PATTERN)
    private String termindato;

    @NotNull
    @Parameter(description = "Utstedt dato (YYYY-MM-DD)")
    @QueryParam("utstedtdato")
    @Pattern(regexp = DATO_PATTERN)
    private String utstedtdato;

    @Parameter(description = "Begrunnelse, fx FAGSYSTEM-nr")
    @FormParam("begrunnelse")
    @Pattern(regexp = FRITEKST)
    private String begrunnelse;

    public SaksnummerTermindatoDto(@NotNull String saksnummer, @NotNull String termindato, @NotNull String utstedtdato, String begrunnelse) {
        this.saksnummer = saksnummer;
        this.termindato = termindato;
        this.utstedtdato = utstedtdato;
        this.begrunnelse = begrunnelse;
    }

    public SaksnummerTermindatoDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.SAKSNUMMER, saksnummer);
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public LocalDate getTermindato() {
        return LocalDate.parse(termindato);
    }

    public LocalDate getUtstedtdato() {
        return LocalDate.parse(utstedtdato);
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
