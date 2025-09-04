package no.nav.foreldrepenger.domene.rest.dto.fordeling;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public class FordelBeregningsgrunnlagAndelDto extends FordelRedigerbarAndelDto {

    @Valid
    @NotNull
    private FordelFastsatteVerdierDto fastsatteVerdier;
    @ValidKodeverk
    private Inntektskategori forrigeInntektskategori;
    private Integer forrigeRefusjonPrÅr;
    private Integer forrigeArbeidsinntektPrÅr;

    public FordelBeregningsgrunnlagAndelDto() {
    }

    public FordelBeregningsgrunnlagAndelDto(FordelFastsatteVerdierDto fastsatteVerdier,
                                                         Inntektskategori forrigeInntektskategori,
                                                         Integer forrigeRefusjonPrÅr,
                                                         Integer forrigeArbeidsinntektPrÅr) {
        this.fastsatteVerdier = fastsatteVerdier;
        this.forrigeInntektskategori = forrigeInntektskategori;
        this.forrigeRefusjonPrÅr = forrigeRefusjonPrÅr;
        this.forrigeArbeidsinntektPrÅr = forrigeArbeidsinntektPrÅr;
    }

    public FordelFastsatteVerdierDto getFastsatteVerdier() {
        return fastsatteVerdier;
    }

    public Inntektskategori getForrigeInntektskategori() {
        return forrigeInntektskategori;
    }

    public Integer getForrigeRefusjonPrÅr() {
        return forrigeRefusjonPrÅr;
    }

    public Integer getForrigeArbeidsinntektPrÅr() {
        return forrigeArbeidsinntektPrÅr;
    }
}
