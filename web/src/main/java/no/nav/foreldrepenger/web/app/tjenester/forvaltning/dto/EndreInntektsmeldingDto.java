package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.InputValideringRegexDato.DATO_PATTERN;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class EndreInntektsmeldingDto implements AbacDto {

    @NotNull
    @QueryParam("behandlingUuid")
    @Valid
    private UUID behandlingUuid;

    @NotNull
    @QueryParam("journalpostid")
    @Digits(integer = 18, fraction = 0)
    private String journalpostId;

    @Parameter(description = "YYYY-MM-DD")
    @QueryParam("refusjonOpphoerFom")
    @Pattern(regexp = DATO_PATTERN)
    private String refusjonOpphørFom;

    @QueryParam("refusjonPrMndFraStart")
    @Min(value = 0)
    @Max(value = Long.MAX_VALUE)
    private Long refusjonPrMndFraStart;

    @Valid
    @QueryParam("refusjonsendringer")
    @Size(max = 20)
    private List<RefusjonsendringDto> refusjonsendringer;

    public EndreInntektsmeldingDto() {
        // Jackson
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett();
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public LocalDate getRefusjonOpphørFom() {
        return getLocalDate(refusjonOpphørFom);
    }

    public List<RefusjonsendringDto> getRefusjonsendringer() {
        return refusjonsendringer == null ? Collections.emptyList() : refusjonsendringer;
    }

    private LocalDate getLocalDate(String datoString) {
        if (datoString != null) {
            return LocalDate.parse(datoString, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return null;
    }

    public Long getRefusjonPrMndFraStart() {
        return refusjonPrMndFraStart;
    }
}
