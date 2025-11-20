package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public class FastsettBeregningsgrunnlagAndelDto extends RedigerbarAndelDto {

    @Valid
    @NotNull
    private FastsatteVerdierDto fastsatteVerdier;
    @ValidKodeverk
    private Inntektskategori forrigeInntektskategori;
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer forrigeRefusjonPrÅr;
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer forrigeArbeidsinntektPrÅr;

    FastsettBeregningsgrunnlagAndelDto() {
        // Jackson
    }

    public FastsettBeregningsgrunnlagAndelDto(RedigerbarAndelDto andelDto,
                                              FastsatteVerdierDto fastsatteVerdier, Inntektskategori forrigeInntektskategori, Integer forrigeRefusjonPrÅr, Integer forrigeArbeidsinntektPrÅr) {
        super(andelDto.getNyAndel(), andelDto.getArbeidsgiverId(), andelDto.getArbeidsforholdId(),
            andelDto.getAndelsnr(), andelDto.getLagtTilAvSaksbehandler(), andelDto.getAktivitetStatus(), OpptjeningAktivitetType.ARBEID);
        this.fastsatteVerdier = fastsatteVerdier;
        this.forrigeArbeidsinntektPrÅr = forrigeArbeidsinntektPrÅr;
        this.forrigeInntektskategori = forrigeInntektskategori;
        this.forrigeRefusjonPrÅr = forrigeRefusjonPrÅr;
    }

    public FastsatteVerdierDto getFastsatteVerdier() {
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
