package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;

public record ArbeidsforholdDto(@NotNull String arbeidsgiverIdent,
                                String internArbeidsforholdId,
                                String eksternArbeidsforholdId,
                                @NotNull LocalDate fom,
                                @NotNull LocalDate tom,
                                @NotNull BigDecimal stillingsprosent,
                                AksjonspunktÅrsak årsak,
                                @NotNull ArbeidsforholdKomplettVurderingType saksbehandlersVurdering,
                                PermisjonOgMangelDto permisjonOgMangel,
                                String begrunnelse){}
