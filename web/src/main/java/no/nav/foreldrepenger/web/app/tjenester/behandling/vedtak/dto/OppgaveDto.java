package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OppgaveType;
import no.nav.foreldrepenger.web.app.tjenester.dokument.dto.DokumentDto;

public record OppgaveDto(OppgaveType oppgavetype, Beskrivelse nyesteBeskrivelse, List<Beskrivelse> eldreBeskrivelser, DokumentDto hovedDokument,
                         List<DokumentDto> andreDokumenter) {

    public record Beskrivelse(String header, String kommentar) {
    }
}
