package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDate;
import java.util.Objects;


import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;

public record PersonDto(
    String aktørId,
    @NotNull String navn,
    @NotNull String fødselsnummer,
    @NotNull NavBrukerKjønn kjønn,
    String diskresjonskode,
    @NotNull LocalDate fødselsdato,
    LocalDate dødsdato,
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
