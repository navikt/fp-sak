package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;

public record InntektsmeldingDto(@NotNull BigDecimal inntektPrMnd,
                                 BigDecimal refusjonPrMnd,
                                 @NotNull String arbeidsgiverIdent,
                                 String eksternArbeidsforholdId,
                                 String internArbeidsforholdId,
                                 @NotNull String kontaktpersonNavn,
                                 @NotNull String kontaktpersonNummer,
                                 @NotNull String journalpostId,
                                 @NotNull String dokumentId,
                                 @NotNull LocalDate motattDato,
                                 @NotNull LocalDateTime innsendingstidspunkt,
                                 AksjonspunktÅrsak årsak,
                                 String begrunnelse,
                                 ArbeidsforholdKomplettVurderingType saksbehandlersVurdering,
                                 @NotNull String kildeSystem,
                                 LocalDate startDatoPermisjon,
                                 @NotNull List<NaturalYtelse> aktiveNaturalytelser,
                                 @NotNull List<Refusjon> refusjonsperioder,
                                 @NotNull InntektsmeldingInnsendingsårsak innsendingsårsak,
                                 @NotNull List<UUID> tilknyttedeBehandlingIder
                                 ){}
