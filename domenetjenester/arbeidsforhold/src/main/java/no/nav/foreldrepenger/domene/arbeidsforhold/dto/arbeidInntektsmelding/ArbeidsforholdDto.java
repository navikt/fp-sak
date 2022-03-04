package no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;

public record ArbeidsforholdDto(String arbeidsgiverIdent,
                                String internArbeidsforholdId,
                                String eksternArbeidsforholdId,
                                LocalDate fom,
                                LocalDate tom,
                                BigDecimal stillingsprosent,
                                AksjonspunktÅrsak årsak,
                                ArbeidsforholdKomplettVurderingType saksbehandlersVurdering,
                                PermisjonUtenSluttdatoDto permisjonUtenSluttdatoDto,
                                String begrunnelse){}
