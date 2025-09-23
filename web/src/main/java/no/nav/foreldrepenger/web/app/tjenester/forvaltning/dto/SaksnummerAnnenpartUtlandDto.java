package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class SaksnummerAnnenpartUtlandDto implements AbacDto {


    @NotNull
    @Parameter(description = "Saksnummer")
    @QueryParam("saksnummer")
    @Digits(integer = 18, fraction = 0)
    private String saksnummer;

    @NotNull
    @Parameter(description = "Utlandsk ident annen part")
    @QueryParam("identAnnenPart")
    private String identAnnenPart;

    @Parameter(description = "Begrunnelse, fx FAGSYSTEM-nr")
    @QueryParam("begrunnelse")
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
        return TilbakeRestTjeneste.opprett()
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
