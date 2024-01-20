package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import java.time.LocalDate;
import java.util.List;

public record RelaterteYtelserDto(String relatertYtelseNavn, List<TilgrensendeYtelserDto> tilgrensendeYtelserListe) {

    public record TilgrensendeYtelserDto(LocalDate periodeFraDato, LocalDate periodeTilDato, String statusNavn, String saksNummer) { }
}
