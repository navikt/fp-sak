package no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class SekvensIdParam implements AbacDto{

    @NotNull
    @Digits(integer = 1000, fraction = 0)
    @Min(0)
    private final String sekvensId;

    public SekvensIdParam(String sekvensId) {
        this.sekvensId = sekvensId;
    }

    public Long get() {
       return Long.valueOf(sekvensId);
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return TilbakeRestTjeneste.opprett(); //tom, i praksis rollebasert tilgang på JSON-feed
    }
}
