package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.util.InputValideringRegex;

public class SaksnummerAnnenpartUtlandDto implements AbacDto {


    @NotNull
    @Parameter(description = "Saksnummer")
    @QueryParam("saksnummer")
    @Digits(integer = 18, fraction = 0)
    private String saksnummer;

    @NotNull
    @Parameter(description = "Utlandsk ident annen part")
    @QueryParam("identAnnenPart")
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String identAnnenPart;

    @Parameter(description = "Begrunnelse, fx FAGSYSTEM-nr")
    @QueryParam("begrunnelse")
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    public SaksnummerAnnenpartUtlandDto(@NotNull String saksnummer, @NotNull String identAnnenPart, String begrunnelse) {
        this.saksnummer = saksnummer;
        this.identAnnenPart = identAnnenPart;
        this.begrunnelse = begrunnelse;
    }

    public SaksnummerAnnenpartUtlandDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
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
