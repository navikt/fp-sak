package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.InputValideringRegexDato.DATO_PATTERN;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;

public class RefusjonsendringDto {

    @NotNull
    @Parameter(description = "YYYY-MM-DD")
    @QueryParam("fom")
    @Pattern(regexp = DATO_PATTERN)
    private String fom;

    @NotNull
    @QueryParam("beløp")
    @Min(value = 0)
    @Max(value = Long.MAX_VALUE)
    private Long beløp;

    public RefusjonsendringDto() {
        // Jackson
    }

    public Long getBeløp() {
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
