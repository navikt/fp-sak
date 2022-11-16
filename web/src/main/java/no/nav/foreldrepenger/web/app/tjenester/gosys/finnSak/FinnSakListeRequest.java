package no.nav.foreldrepenger.web.app.tjenester.gosys.finnSak;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

public record FinnSakListeRequest(@Valid @Pattern(regexp = "^\\d{13}$", message = "aktørId ${validatedValue} har ikke gyldig verdi (pattern '{regexp}')") String aktørId) {
}
