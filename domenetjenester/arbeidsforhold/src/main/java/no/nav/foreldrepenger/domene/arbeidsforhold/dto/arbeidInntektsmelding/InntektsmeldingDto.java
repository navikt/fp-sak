package no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InntektsmeldingDto(BigDecimal inntektPrMnd,
                                BigDecimal refusjonPrMnd,
                                String arbeidsgiverIdent,
                                String eksternArbeidsforholdId,
                                String internArbeidsforholdId,
                                String kontaktpersonNavn,
                                String kontaktpersonNummer,
                                String journalpostId,
                                String dokumentId,
                                LocalDate motattDato,
                                LocalDateTime innsendingstidspunkt){}
