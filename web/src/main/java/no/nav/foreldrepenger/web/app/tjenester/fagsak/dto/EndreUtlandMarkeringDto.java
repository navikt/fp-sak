package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.util.Set;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public record EndreUtlandMarkeringDto(@JsonProperty("saksnummer") @NotNull @Digits(integer = 18, fraction = 0) String saksnummer,
                                      @Size(max = 25) Set<@ValidKodeverk FagsakMarkering> fagsakMarkeringer) {

}
