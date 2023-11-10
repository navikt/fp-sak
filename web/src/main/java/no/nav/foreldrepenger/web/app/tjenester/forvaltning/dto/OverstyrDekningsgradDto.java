package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;

import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class OverstyrDekningsgradDto implements AbacDto {

    @NotNull
    @QueryParam("saksnummer")
    @Digits(integer = 18, fraction = 0)
    private String saksnummer;

    @NotNull
    @QueryParam("dekningsgrad")
    @Digits(integer = 3, fraction = 0)
    private String dekningsgrad;

    public OverstyrDekningsgradDto(@NotNull String saksnummer, @NotNull String dekningsgrad) {
        this.saksnummer = saksnummer;
        this.dekningsgrad = dekningsgrad;
    }

    public OverstyrDekningsgradDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.SAKSNUMMER, saksnummer);
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public String getDekningsgrad() {
        return dekningsgrad;
    }
}
