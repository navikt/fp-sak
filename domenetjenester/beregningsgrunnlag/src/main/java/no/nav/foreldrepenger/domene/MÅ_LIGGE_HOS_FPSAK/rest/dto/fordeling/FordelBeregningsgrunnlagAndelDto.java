package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.fordeling;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Inntektskategori;

public class FordelBeregningsgrunnlagAndelDto extends FordelRedigerbarAndelDto {

    @Valid
    @NotNull
    private FordelFastsatteVerdierDto fastsatteVerdier;
    private Inntektskategori forrigeInntektskategori;
    private Integer forrigeRefusjonPrÅr;
    private Integer forrigeArbeidsinntektPrÅr;

    FordelBeregningsgrunnlagAndelDto() { // NOSONAR
        // Jackson
    }

    public FordelBeregningsgrunnlagAndelDto(FordelRedigerbarAndelDto andelDto,
                                            FordelFastsatteVerdierDto fastsatteVerdier, Inntektskategori forrigeInntektskategori, Integer forrigeRefusjonPrÅr, Integer forrigeArbeidsinntektPrÅr) {
        super(andelDto.getNyAndel(), andelDto.getArbeidsgiverId(), andelDto.getArbeidsforholdId(),
            andelDto.getAndelsnr(), andelDto.getLagtTilAvSaksbehandler(), andelDto.getAktivitetStatus(), OpptjeningAktivitetType.ARBEID);
        this.fastsatteVerdier = fastsatteVerdier;
        this.forrigeArbeidsinntektPrÅr = forrigeArbeidsinntektPrÅr;
        this.forrigeInntektskategori = forrigeInntektskategori;
        this.forrigeRefusjonPrÅr = forrigeRefusjonPrÅr;
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
