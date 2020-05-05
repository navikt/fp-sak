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

public class AvstemmingEnkeltSakDto implements AbacDto {

    @NotNull
    @Parameter(description = "key (secret)")
    @QueryParam("key")
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String key;

    @NotNull
    @QueryParam("saksnummer")
    @Digits(integer = 18, fraction = 0)
    private String saksnummer;

    public AvstemmingEnkeltSakDto(@NotNull String key, @NotNull String saksnummer) {
        this.key = key;
        this.saksnummer = saksnummer;
    }

    public AvstemmingEnkeltSakDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.SAKSNUMMER, saksnummer);
    }

    public String getKey() {
        return key;
    }

    public String getSaksnummer() {
        return saksnummer;
    }
}
