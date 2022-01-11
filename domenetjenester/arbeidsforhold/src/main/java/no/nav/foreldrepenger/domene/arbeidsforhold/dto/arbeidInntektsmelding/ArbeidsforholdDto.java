package no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ArbeidsforholdDto(String arbeidsgiverIdent,
                                String internArbeidsforholdId,
                                String eksternArbeidsforholdId,
                                LocalDate fom,
                                LocalDate tom,
                                BigDecimal stillingsprosent,
                                AksjonspunktÅrsak årsak,
                                ArbeidsforholdKomplettVurderingType saksbehandlersVurdering,
                                String begrunnelse){}
