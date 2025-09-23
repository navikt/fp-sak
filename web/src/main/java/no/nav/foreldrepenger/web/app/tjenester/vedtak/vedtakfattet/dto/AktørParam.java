package no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto;

import java.util.Optional;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.util.InputValideringRegex;

public class AktørParam implements AbacDto {

    @Size(max = 100)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private final String aktørId;

    public AktørParam(String aktørId) {
        this.aktørId = aktørId;
    }

    public Optional<AktørId> get() {
        if (aktørId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AktørId(aktørId));
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett(); // tom, i praksis rollebasert tilgang på JSON-feed
    }
}
