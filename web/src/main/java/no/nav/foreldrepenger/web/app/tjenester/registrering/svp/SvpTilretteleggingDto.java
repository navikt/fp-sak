package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SvpTilretteleggingDto {

    private SvpTilretteleggingTypeDto tilretteleggingType;

    private LocalDate dato;

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
