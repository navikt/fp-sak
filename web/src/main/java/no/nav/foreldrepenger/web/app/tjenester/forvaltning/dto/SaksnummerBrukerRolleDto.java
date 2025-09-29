package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;

import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class SaksnummerBrukerRolleDto implements AbacDto {

    public enum BrukerRolleDto { MOR, FAR, MEDMOR }

    @NotNull
    @QueryParam("saksnummer")
    @Digits(integer = 18, fraction = 0)
    private String saksnummer;

    @NotNull
    @QueryParam("rolle")
    @Valid
    private BrukerRolleDto rolle;

    public SaksnummerBrukerRolleDto(@NotNull String saksnummer, @NotNull BrukerRolleDto rolle) {
        this.saksnummer = saksnummer;
        this.rolle = rolle;
    }

    public SaksnummerBrukerRolleDto() {
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public BrukerRolleDto getRolle() {
        return rolle;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.SAKSNUMMER, saksnummer);
    }
}
