package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto;

import jakarta.validation.constraints.NotNull;

public class BeregningsresultatEngangsstønadDto {

    @NotNull private Long beregnetTilkjentYtelse;
    @NotNull private Long satsVerdi;
    @NotNull private Integer antallBarn;

    public BeregningsresultatEngangsstønadDto(Long beregnetTilkjentYtelse, Long satsVerdi, Integer antallBarn) {
        this.beregnetTilkjentYtelse = beregnetTilkjentYtelse;
        this.satsVerdi = satsVerdi;
        this.antallBarn = antallBarn;
    }

    public BeregningsresultatEngangsstønadDto() {
    }

    public void setBeregnetTilkjentYtelse(Long beregnetTilkjentYtelse) {
        this.beregnetTilkjentYtelse = beregnetTilkjentYtelse;
    }

    public void setSatsVerdi(Long satsVerdi) {
        this.satsVerdi = satsVerdi;
    }

    public void setAntallBarn(Integer antallBarn) {
        this.antallBarn = antallBarn;
    }

    public Long getBeregnetTilkjentYtelse() {
        return beregnetTilkjentYtelse;
    }

    public Long getSatsVerdi() {
        return satsVerdi;
    }

    public Integer getAntallBarn() {
        return antallBarn;
    }

}
