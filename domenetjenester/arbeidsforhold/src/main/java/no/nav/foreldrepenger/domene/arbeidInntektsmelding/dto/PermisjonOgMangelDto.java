package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;

public record PermisjonOgMangelDto(@NotNull LocalDate permisjonFom,
                                   LocalDate permisjonTom,
                                   @NotNull PermisjonsbeskrivelseType type,
                                   AksjonspunktÅrsak årsak,
                                   BekreftetPermisjonStatus permisjonStatus){}
