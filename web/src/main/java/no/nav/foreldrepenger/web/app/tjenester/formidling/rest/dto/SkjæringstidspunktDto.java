package no.nav.foreldrepenger.web.app.tjenester.formidling.rest.dto;

import java.time.LocalDate;

public record SkjæringstidspunktDto(LocalDate dato, boolean utenMinsterett) {

}
