package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;

public record ArbeidsforholdDto(@NotNull String arbeidsgiverIdent,
                                String internArbeidsforholdId,
                                String eksternArbeidsforholdId,
                                @NotNull LocalDate fom,
                                @NotNull LocalDate tom,
                                BigDecimal stillingsprosent,
                                AksjonspunktÅrsak årsak,
                                ArbeidsforholdKomplettVurderingType saksbehandlersVurdering,
                                PermisjonOgMangelDto permisjonOgMangel,
                                @NotNull List<@Valid PermisjonDto> permisjoner,
                                String begrunnelse){}
