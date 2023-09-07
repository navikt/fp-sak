package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.time.LocalDateTime;

import no.nav.foreldrepenger.domene.typer.Beløp;

record InntektsmeldingDto(Arbeidsgiver arbeidsgiver, LocalDateTime innsendingstidspunkt, Beløp inntekt) {
}
