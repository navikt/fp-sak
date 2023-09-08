package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import no.nav.foreldrepenger.domene.typer.JournalpostId;

record InntektsmeldingDto(JournalpostId journalpostId, Arbeidsgiver arbeidsgiver, LocalDateTime innsendingstidspunkt, BigDecimal inntekt) {
}
