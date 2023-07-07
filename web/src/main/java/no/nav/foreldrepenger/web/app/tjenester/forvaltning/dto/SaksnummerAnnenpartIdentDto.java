package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import static no.nav.vedtak.util.InputValideringRegex.FRITEKST;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class SaksnummerAnnenpartIdentDto implements AbacDto {


    @NotNull
    @Parameter(description = "Saksnummer")
    @QueryParam("saksnummer")
    @Digits(integer = 18, fraction = 0)
    private String saksnummer;

    @NotNull
    @Parameter(description = "FNR annen part")
    @QueryParam("identAnnenPart")
    @Digits(integer = 11, fraction = 0)
    private String identAnnenPart;

    @Parameter(description = "Begrunnelse, fx FAGSYSTEM-nr")
    @FormParam("begrunnelse")
    @Pattern(regexp = FRITEKST)
    private String begrunnelse;

    public SaksnummerAnnenpartIdentDto(@NotNull String saksnummer, @NotNull String identAnnenPart, String begrunnelse) {
        this.saksnummer = saksnummer;
        this.identAnnenPart = identAnnenPart;
        this.begrunnelse = begrunnelse;
    }

    public SaksnummerAnnenpartIdentDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.FNR, identAnnenPart)
            .leggTil(AppAbacAttributtType.SAKSNUMMER, saksnummer);
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getIdentAnnenPart() {
        return identAnnenPart;
    }
}
