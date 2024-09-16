package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;

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
                                 ArbeidsforholdKomplettVurderingType saksbehandlersVurdering,
                                 String kildeSystem,
                                 LocalDate startDatoPermisjon,
                                 List<NaturalYtelse> bortfalteNaturalytelser,
                                 List<NaturalYtelse> aktiveNaturalytelser,
                                 List<Refusjon> refusjonsperioder,
                                 InntektsmeldingInnsendingsårsak innsendingsårsak,
                                 List<UUID> tilknyttedeBehandlingIder
                                 ){}
