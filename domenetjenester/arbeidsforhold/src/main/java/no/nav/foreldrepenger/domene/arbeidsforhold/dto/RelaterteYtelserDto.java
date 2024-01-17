package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import java.time.LocalDate;
import java.util.List;

public record RelaterteYtelserDto(String relatertYtelseType, String relatertYtelseNavn, List<TilgrensendeYtelserDto> tilgrensendeYtelserListe) {

    public record TilgrensendeYtelserDto(LocalDate periodeFraDato, LocalDate periodeTilDato, String status, String statusNavn, String saksNummer) { }
}
