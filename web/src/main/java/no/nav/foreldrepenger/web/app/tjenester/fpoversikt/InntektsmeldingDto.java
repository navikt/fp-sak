package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.math.BigDecimal;
import java.time.LocalDateTime;

record InntektsmeldingDto(Arbeidsgiver arbeidsgiver, LocalDateTime innsendingstidspunkt, BigDecimal inntekt) {
}
