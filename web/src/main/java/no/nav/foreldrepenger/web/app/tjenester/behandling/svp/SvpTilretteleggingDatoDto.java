package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import java.math.BigDecimal;
import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;

public class SvpTilretteleggingDatoDto {
    private LocalDate fom;
    private TilretteleggingType type;
    private BigDecimal stillingsprosent;

    public SvpTilretteleggingDatoDto() {
        //nix
    }

    SvpTilretteleggingDatoDto(LocalDate fom, TilretteleggingType type, BigDecimal stillingsprosent) {
        this.fom = fom;
        this.type = type;
        this.stillingsprosent = stillingsprosent;
    }

    public LocalDate getFom() {
        return fom;
    }

    public TilretteleggingType getType() {
        return type;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public SvpTilretteleggingDatoDto setFom(LocalDate fom) {
        this.fom = fom;
        return this;
    }

    public SvpTilretteleggingDatoDto setType(TilretteleggingType type) {
        this.type = type;
        return this;
    }

    public SvpTilretteleggingDatoDto setStillingsprosent(BigDecimal stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
        return this;
    }
}
