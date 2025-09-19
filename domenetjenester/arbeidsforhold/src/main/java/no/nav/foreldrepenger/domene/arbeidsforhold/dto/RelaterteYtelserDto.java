package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record RelaterteYtelserDto(@NotNull String relatertYtelseNavn, @NotNull List<TilgrensendeYtelserDto> tilgrensendeYtelserListe) {

    public record TilgrensendeYtelserDto(@NotNull LocalDate periodeFraDato, @NotNull LocalDate periodeTilDato, @NotNull String statusNavn, String saksNummer) { }
}
