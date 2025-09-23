package no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class MaxAntallParam implements AbacDto{

    @Digits(integer = 1000, fraction = 0)
    @Min(1)
    @Max(1000)
    private final String maxAntall;

    public MaxAntallParam(String maxAntall) {
        this.maxAntall = maxAntall;
    }

    public Long get() {
        if (maxAntall.isEmpty()) {
            return 100L;
        }
        return Long.valueOf(maxAntall);
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return TilbakeRestTjeneste.opprett(); //tom, i praksis rollebasert tilgang på JSON-feed
    }
}
