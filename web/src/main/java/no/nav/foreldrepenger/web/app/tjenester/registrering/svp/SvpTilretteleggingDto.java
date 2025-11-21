package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class SvpTilretteleggingDto {

    @Valid
    private SvpTilretteleggingTypeDto tilretteleggingType;

    private LocalDate dato;

    @Valid
    @Min(0)
    @Max(100)
    private BigDecimal stillingsprosent;

    public SvpTilretteleggingTypeDto getTilretteleggingType() {
        return tilretteleggingType;
    }

    public void setTilretteleggingType(SvpTilretteleggingTypeDto tilretteleggingType) {
        this.tilretteleggingType = tilretteleggingType;
    }

    public LocalDate getDato() {
        return dato;
    }

    public void setDato(LocalDate dato) {
        this.dato = dato;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public void setStillingsprosent(BigDecimal stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }
}
