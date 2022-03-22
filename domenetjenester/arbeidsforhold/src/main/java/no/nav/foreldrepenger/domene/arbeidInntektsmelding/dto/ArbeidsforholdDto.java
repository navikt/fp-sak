package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

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
                                PermisjonOgMangelDto permisjonOgMangel,
                                String begrunnelse){}
