package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class KobleFagsakerDto implements AbacDto {

    @NotNull
    @QueryParam("saksnummer1")
    @Digits(integer = 18, fraction = 0)
    private String saksnummer1;

    @NotNull
    @QueryParam("saksnummer2")
    @Digits(integer = 18, fraction = 0)
    private String saksnummer2;

    public KobleFagsakerDto(@NotNull String saksnummer1, @NotNull String saksnummer2) {
        this.saksnummer1 = saksnummer1;
        this.saksnummer2 = saksnummer2;
    }

    public KobleFagsakerDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.SAKSNUMMER, saksnummer1)
            .leggTil(AppAbacAttributtType.SAKSNUMMER, saksnummer2);
    }

    public String getSaksnummer1() {
        return saksnummer1;
    }

    public String getSaksnummer2() {
        return saksnummer2;
    }
}
