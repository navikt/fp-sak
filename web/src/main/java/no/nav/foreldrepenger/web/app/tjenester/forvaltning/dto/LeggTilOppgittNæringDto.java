package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.InputValideringRegexDato.DATO_PATTERN;
import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.LeggTilOppgittNæringDto.Utfall.JA;
import static no.nav.vedtak.util.InputValideringRegex.FRITEKST;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class LeggTilOppgittNæringDto implements AbacDto {

    @Valid
    @NotNull
    @FormParam("behandlingUuid")
    private UUID behandlingUuid;

    @NotNull
    @FormParam("typeKode")
    @Pattern(regexp = "^(?:ANNEN|DAGMAMMA|FISKE|JORDBRUK_SKOGBRUK)$")
    @Schema(allowableValues = { "ANNEN", "DAGMAMMA", "FISKE", "JORDBRUK_SKOGBRUK" })
    private String typeKode;

    @NotNull
    @Parameter(description = "YYYY-MM-DD")
    @Pattern(regexp = DATO_PATTERN)
    @FormParam("fom")
    private String fom;

    @Parameter(description = "YYYY-MM-DD")
    @Pattern(regexp = DATO_PATTERN)
    @FormParam("tom")
    private String tom;

    @Pattern(regexp = FRITEKST)
    @FormParam("orgnummer")
    private String orgnummer;

    @Pattern(regexp = FRITEKST)
    @FormParam("regnskapNavn")
    private String regnskapNavn;

    @Pattern(regexp = FRITEKST)
    @FormParam("regnskapTlf")
    private String regnskapTlf;

    @Valid
    @DefaultValue("NEI")
    @FormParam("nyoppstartet")
    private Utfall nyoppstartet;

    @Valid
    @DefaultValue("NEI")
    @FormParam("varigEndring")
    private Utfall varigEndring;

    @Valid
    @DefaultValue("NEI")
    @FormParam("nyIArbeidslivet")
    private Utfall nyIArbeidslivet;

    @Valid
    @DefaultValue("NEI")
    @FormParam("erRelasjon")
    private Utfall erRelasjon;

    @Parameter(description = "YYYY-MM-DD")
    @Pattern(regexp = DATO_PATTERN)
    @FormParam("endringsDato")
    private String endringsDato;

    @Pattern(regexp = FRITEKST)
    @FormParam("begrunnelse")
    private String begrunnelse;

    @Min(0)
    @Max(Long.MAX_VALUE)
    @FormParam("bruttoBeløp")
    private long bruttoBeløp;

    @Override
    public AbacDataAttributter abacAttributter() {
        var abac = AbacDataAttributter.opprett();
        abac.leggTil(AppAbacAttributtType.BEHANDLING_UUID, behandlingUuid);
        return abac;
    }

    @JsonIgnore
    @AssertTrue(message = "Når [varigEndring] er JA, må også [endringsDato] være satt!")
    public boolean isEndringsdatoSattVedVarigEndringOK() {
        if (JA.equals(varigEndring)) {
            return erDatoSatt(endringsDato);
        }
        return true;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public String getTypeKode() {
        return typeKode;
    }

    public LocalDate getFom() {
        return getLocalDate(fom);
    }

    public LocalDate getTom() {
        return getLocalDate(tom);
    }

    public String getOrgnummer() {
        return orgnummer;
    }

    public String getRegnskapNavn() {
        return regnskapNavn;
    }

    public String getRegnskapTlf() {
        return regnskapTlf;
    }

    public Utfall getNyoppstartet() {
        return nyoppstartet;
    }

    public Utfall getVarigEndring() {
        return varigEndring;
    }

    public LocalDate getEndringsDato() {
        return getLocalDate(endringsDato);
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public long getBruttoBeløp() {
        return bruttoBeløp;
    }

    public Utfall getNyIArbeidslivet() {
        return nyIArbeidslivet;
    }

    public Utfall getErRelasjon() {
        return erRelasjon;
    }

    private LocalDate getLocalDate(String datoString) {
        if (erDatoSatt(datoString)) {
            return LocalDate.parse(datoString);
        }
        return null;
    }

    private boolean erDatoSatt(String datoString) {
        return datoString != null && !datoString.isEmpty();
    }

    public enum Utfall {
        JA,
        NEI
    }
}
