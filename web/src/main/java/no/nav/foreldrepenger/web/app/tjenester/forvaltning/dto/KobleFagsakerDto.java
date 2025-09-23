package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;

import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
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
        return TilbakeRestTjeneste.opprett()
            .leggTil(AppAbacAttributtType.SAKSNUMMER, saksnummer1);
    }

    public String getSaksnummer1() {
        return saksnummer1;
    }

    public String getSaksnummer2() {
        return saksnummer2;
    }
}
