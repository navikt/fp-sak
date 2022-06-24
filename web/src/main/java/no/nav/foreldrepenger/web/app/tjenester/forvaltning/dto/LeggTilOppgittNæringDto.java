package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.LeggTilOppgittNæringDto.Utfall.JA;
import static no.nav.vedtak.util.InputValideringRegex.FRITEKST;

import java.time.LocalDate;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public record LeggTilOppgittNæringDto(
    @Valid @NotNull UUID behandlingUuid,
    @NotNull @Pattern(regexp = "^(?:ANNEN|DAGMAMMA|FISKE|JORDBRUK_SKOGBRUK)$") String typeKode,
    @NotNull LocalDate fom,
    LocalDate tom,
    @Pattern(regexp = FRITEKST) @Schema(defaultValue = "123456789") String orgnummer,
    @Pattern(regexp = FRITEKST) @Schema(defaultValue = "regnskapNavn") String regnskapNavn,
    @Pattern(regexp = FRITEKST) @Schema(defaultValue = "99999999") String regnskapTlf,
    @Valid @Schema(defaultValue = "NEI") Utfall nyoppstartet,
    @Valid @Schema(defaultValue = "NEI") Utfall varigEndring,
    LocalDate endringsDato,
    @Pattern(regexp = FRITEKST) @Schema(defaultValue = "begrunnelse") String begrunnelse,
    @Min(0) @Max(Long.MAX_VALUE) @Schema(defaultValue = "0") long bruttoBeløp) implements AbacDto {

    @Override
    public AbacDataAttributter abacAttributter() {
        var abac = AbacDataAttributter.opprett();
        abac.leggTil(AppAbacAttributtType.BEHANDLING_UUID, behandlingUuid());
        return abac;
    }

    @JsonIgnore
    @AssertTrue(message = "Når [varigEndring] er JA, må også [endringsDato] være satt!")
    public boolean isEndringsdatoSattVedVarigEndringOK() {
        if (JA.equals(varigEndring())) {
            return endringsDato() != null;
        }
        return true;
    }

    public enum Utfall {
        JA,
        NEI
    }
}
