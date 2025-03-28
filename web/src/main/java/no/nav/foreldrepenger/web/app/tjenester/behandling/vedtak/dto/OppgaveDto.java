package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OppgaveType;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

public record OppgaveDto(OppgaveType oppgavetype, String beskrivelse, Beskrivelse nyesteBeskrivelse, List<Beskrivelse> eldreBeskrivelser, List<Dokument> dokumenter) {

    public record Beskrivelse(String header, List<String> kommentarer) {
    }

    public record Dokument(JournalpostId journalpostId, String dokumentId, String tittel) {
    }
}
