package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.UtlandMarkering;

public record EndreUtlandMarkeringDto(@JsonProperty("saksnummer") @NotNull @Digits(integer = 18, fraction = 0) String saksnummer, @NotNull@Valid UtlandMarkering utlandMarkering) {
}
