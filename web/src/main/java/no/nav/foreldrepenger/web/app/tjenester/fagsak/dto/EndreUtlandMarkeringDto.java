package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;

public record EndreUtlandMarkeringDto(@JsonProperty("saksnummer") @NotNull @Digits(integer = 18, fraction = 0) String saksnummer,
                                      @NotNull @Valid FagsakMarkering fagsakMarkering) {
}
