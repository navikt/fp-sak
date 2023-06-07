package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.vedtak.util.InputValideringRegex;

// Begynner med 500 tegn så får vi se behovet. Tabell har max 4000 tegn
public record LagreFagsakNotatDto(@JsonProperty("saksnummer") @NotNull @Digits(integer = 18, fraction = 0) String saksnummer,
                                  @NotNull @Size(max = 500) @Pattern(regexp = InputValideringRegex.FRITEKST) String notat) {
}
