package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;

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
                                 LocalDateTime innsendingstidspunkt,
                                 AksjonspunktÅrsak årsak,
                                 String begrunnelse,
                                 ArbeidsforholdKomplettVurderingType saksbehandlersVurdering){}
