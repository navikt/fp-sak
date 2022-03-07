package no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding;

import java.time.LocalDate;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;

public record PermisjonUtenSluttdatoDto(LocalDate permisjonFom,
                                        BekreftetPermisjonStatus permisjonStatus){}
