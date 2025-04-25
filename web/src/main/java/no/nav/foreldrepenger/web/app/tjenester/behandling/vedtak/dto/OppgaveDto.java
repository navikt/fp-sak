package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto;

import java.util.List;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OppgaveType;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

public record OppgaveDto(OppgaveId oppgaveId, OppgaveType oppgavetype, List<Beskrivelse> beskrivelser, List<Dokument> dokumenter) {

    public record OppgaveId(@NotNull @Digits(integer = 32, fraction = 0) @Min(1) @JsonValue String id) {
    }

    public record Beskrivelse(String header, List<String> kommentarer) {
    }

    public record Dokument(JournalpostId journalpostId, String dokumentId, String tittel) {
    }
}
