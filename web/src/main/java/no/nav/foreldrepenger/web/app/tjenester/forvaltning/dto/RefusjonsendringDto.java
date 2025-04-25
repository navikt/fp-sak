package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.InputValideringRegexDato.DATO_PATTERN;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;

public class RefusjonsendringDto {

    @NotNull
    @Parameter(description = "YYYY-MM-DD")
    @QueryParam("fom")
    @Pattern(regexp = DATO_PATTERN)
    String fom;

    @NotNull
    @QueryParam("beløp")
    @Digits(integer = 10, fraction = 2)
    Double beløp;

    public RefusjonsendringDto() {
        // Jackson
    }

    public Double getBeløp() {
        return beløp;
    }

    public LocalDate getFom() {
        return getLocalDate(fom);
    }

    public LocalDate getLocalDate(String dato){
        if (dato != null) {
            return LocalDate.parse(dato, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return null;
    }
}
