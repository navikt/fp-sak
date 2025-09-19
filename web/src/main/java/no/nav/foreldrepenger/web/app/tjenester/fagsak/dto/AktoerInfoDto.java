package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AktoerInfoDto(String aktørId, @NotNull PersonDto person, @NotNull List<FagsakSøkDto> fagsaker) {
}
