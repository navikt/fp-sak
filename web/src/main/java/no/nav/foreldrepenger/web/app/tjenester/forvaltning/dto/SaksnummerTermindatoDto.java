package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import java.time.LocalDate;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class SaksnummerTermindatoDto implements AbacDto {

    private static final String DATO_PATTERN = "(\\d{4}-\\d{2}-\\d{2})";

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

    @Parameter(description = "Begrunnelse, fx FAGSYSTEM-nr")
    @QueryParam("begrunnelse")
    private String begrunnelse;

    public SaksnummerTermindatoDto(@NotNull String saksnummer, @NotNull String termindato, String begrunnelse) {
        this.saksnummer = saksnummer;
        this.termindato = termindato;
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

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
