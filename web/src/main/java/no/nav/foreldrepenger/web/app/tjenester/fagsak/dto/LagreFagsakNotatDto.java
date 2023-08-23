package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.vedtak.util.InputValideringRegex;

// Begynner med 500 tegn så får vi se behovet. Tabell har max 4000 tegn
public record LagreFagsakNotatDto(@JsonProperty("saksnummer") @NotNull @Digits(integer = 18, fraction = 0) String saksnummer,
                                  @NotNull @Size(max = 1001) @Pattern(regexp = InputValideringRegex.FRITEKST) String notat) {
}
