package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import java.time.LocalDate;

import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;

public record PermisjonOgMangelDto(LocalDate permisjonFom,
                                   LocalDate permisjonTom,
                                   PermisjonsbeskrivelseType type,
                                   AksjonspunktÅrsak årsak,
                                   BekreftetPermisjonStatus permisjonStatus){}
