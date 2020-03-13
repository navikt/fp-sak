package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import java.math.BigDecimal;
import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;

public class SvpTilretteleggingDatoDto {
    private LocalDate fom;
    private TilretteleggingType type;
    private BigDecimal stillingsprosent;
    private BigDecimal overstyrtUtbetalingsgrad;

    public SvpTilretteleggingDatoDto() {
        //nix
    }

    SvpTilretteleggingDatoDto(LocalDate fom, TilretteleggingType type, BigDecimal stillingsprosent) {
        this(fom, type, stillingsprosent, null);
    }

    SvpTilretteleggingDatoDto(LocalDate fom, TilretteleggingType type, BigDecimal stillingsprosent, BigDecimal overstyrtUtbetalingsgrad) {
        this.fom = fom;
        this.type = type;
        this.stillingsprosent = stillingsprosent;
        this.overstyrtUtbetalingsgrad = overstyrtUtbetalingsgrad;
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

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public void setType(TilretteleggingType type) {
        this.type = type;
    }

    public void setStillingsprosent(BigDecimal stillingsprosent) {
        this.stillingsprosent = stillingsprosent;
    }

    public BigDecimal getOverstyrtUtbetalingsgrad() {
        return overstyrtUtbetalingsgrad;
    }

    public void setOverstyrtUtbetalingsgrad(BigDecimal overstyrtUtbetalingsgrad) {
        this.overstyrtUtbetalingsgrad = overstyrtUtbetalingsgrad;
    }

}
