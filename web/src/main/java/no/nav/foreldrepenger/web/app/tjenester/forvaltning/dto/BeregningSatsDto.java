package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.InputValideringRegexDato.DATO_PATTERN;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class BeregningSatsDto implements AbacDto {

    @NotNull
    @QueryParam("satsType")
    @ValidKodeverk
    private BeregningSatsType satsType;

    @NotNull
    @Parameter(description = "YYYY-MM-DD")
    @QueryParam("satsFom")
    @Pattern(regexp = DATO_PATTERN)
    private String satsFom;

    @Parameter(description = "YYYY-MM-DD")
    @QueryParam("satsTom")
    @Pattern(regexp = DATO_PATTERN)
    private String satsTom;

    @NotNull
    @Parameter(description = "Minumum satsverdi er satt til 75000")
    @QueryParam("satsVerdi")
    @DefaultValue("75000")
    @Min(75000)
    @Max(Long.MAX_VALUE)
    private Long satsVerdi;

    public BeregningSatsDto(BeregningSats sats) {
        this.satsFom = sats.getPeriode().getFomDato().toString();
        this.satsTom = sats.getPeriode().getTomDato().toString();
        this.satsType = sats.getSatsType();
        this.satsVerdi = sats.getVerdi();
    }

    public BeregningSatsDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett();
    }

    public Long getSatsVerdi() {
        return satsVerdi;
    }

    public BeregningSatsType getSatsType() {
        return satsType;
    }

    public LocalDate getSatsFom() {
        return getLocalDate(satsFom);
    }

    public LocalDate getSatsTom() {
        return getLocalDate(satsTom);
    }

    private LocalDate getLocalDate(String datoString) {
        if (datoString != null) {
            return LocalDate.parse(datoString, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return null;
    }

    @Override
    public String toString() {
        return "BeregningSatsDto{" +
            "satsType='" + satsType + '\'' +
            ", satsFom='" + satsFom + '\'' +
            ", satsTom='" + satsTom + '\'' +
            ", satsVerdi=" + satsVerdi +
            '}';
    }
}
