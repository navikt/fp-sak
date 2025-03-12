package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OppgaveType;

public record OppgaveDto(OppgaveType oppgavetype, String beskrivelse) {
}
