package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import java.time.LocalDate;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;

public record PermisjonUtenSluttdatoDto(LocalDate permisjonFom,
                                        PermisjonsbeskrivelseType type,
                                        BekreftetPermisjonStatus permisjonStatus){}
