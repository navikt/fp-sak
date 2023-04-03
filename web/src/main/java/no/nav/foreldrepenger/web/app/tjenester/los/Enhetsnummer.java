package no.nav.foreldrepenger.web.app.tjenester.los;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class Enhetsnummer implements AbacDto {

    private final static String GYLDIG_ENHETSNUMMER = "[\\d]{4}";

    @JsonProperty("enhetsnummer")
    @NotNull
    @Pattern(regexp = GYLDIG_ENHETSNUMMER)
    public String enhetsnummer;


    public Enhetsnummer() {

    }

    public Enhetsnummer(String enhetsnummer) {
        this.enhetsnummer = enhetsnummer;
    }


    public String getEnhetsnummer() {
        return enhetsnummer;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett(); //denne er tom, nøkkeltall api har i praksis rollebasert tilgangskontroll
    }
}
