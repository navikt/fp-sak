package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;

public record PermisjonDto(@NotNull LocalDate fom,
                           LocalDate tom,
                           @NotNull BigDecimal permisjonsprosent,
                           @NotNull PermisjonsbeskrivelseType type) {}
