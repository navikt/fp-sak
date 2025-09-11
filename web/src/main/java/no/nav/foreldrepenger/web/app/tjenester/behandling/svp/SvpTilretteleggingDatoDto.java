package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public class SvpTilretteleggingDatoDto {
    @NotNull private LocalDate fom;
    @NotNull @ValidKodeverk private TilretteleggingType type;
    private BigDecimal stillingsprosent;
    private BigDecimal overstyrtUtbetalingsgrad;
    @NotNull @ValidKodeverk private SvpTilretteleggingFomKilde kilde;
    private LocalDate mottattDato;

    public SvpTilretteleggingDatoDto() {
        //nix
    }

    SvpTilretteleggingDatoDto(LocalDate fom, TilretteleggingType type, BigDecimal stillingsprosent) {
        this(fom, type, stillingsprosent, null, null, null);
    }

    SvpTilretteleggingDatoDto(LocalDate fom, TilretteleggingType type, BigDecimal stillingsprosent, BigDecimal overstyrtUtbetalingsgrad, SvpTilretteleggingFomKilde kilde,
                              LocalDate mottattDato) {
        this.fom = fom;
        this.type = type;
        this.stillingsprosent = stillingsprosent;
        this.overstyrtUtbetalingsgrad = overstyrtUtbetalingsgrad;
        this.kilde = kilde;
        this.mottattDato = mottattDato;

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

    public SvpTilretteleggingFomKilde getKilde() {
        return kilde;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

}
