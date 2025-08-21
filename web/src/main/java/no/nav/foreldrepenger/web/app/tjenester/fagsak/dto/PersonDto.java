package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDate;
import java.util.Objects;


import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;

public record PersonDto(
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) String aktørId,
    @NotNull String navn,
    @NotNull String fødselsnummer,
    @NotNull NavBrukerKjønn kjønn,
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) String diskresjonskode,
    @NotNull LocalDate fødselsdato,
    @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) LocalDate dødsdato,
    @Deprecated(forRemoval = true) LocalDate dodsdato,
    @NotNull Språkkode språkkode
) {


    @Override
    public String toString() {
        return "PersonDto{fødselsdato=" + fødselsdato + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof PersonDto other && Objects.equals(aktørId, other.aktørId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }
}
