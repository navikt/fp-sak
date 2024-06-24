package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;

public record EndreUtlandMarkeringDto(@JsonProperty("saksnummer") @NotNull @Digits(integer = 18, fraction = 0) String saksnummer,
                                      @Valid FagsakMarkering fagsakMarkering,
                                      @Valid @Size(max = 25) List<FagsakMarkering> fagsakMarkeringer) {

    public List<FagsakMarkering> getMarkeringer() {
        return Optional.ofNullable(fagsakMarkeringer()).filter(l -> !l.isEmpty() || fagsakMarkering() == null)
            .orElseGet(() -> Optional.of(fagsakMarkering()).stream().toList());
    }

    public FagsakMarkering getEnkeltMarkering() {
        return Optional.ofNullable(fagsakMarkeringer()).flatMap(l -> l.stream().findFirst())
            .or(() -> Optional.ofNullable(fagsakMarkering()))
            .orElseThrow(() -> new IllegalArgumentException("Ingen markering angitt"));
    }
}
