package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.fordeling;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Inntektskategori;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public class FordelBeregningsgrunnlagAndelDto extends FordelRedigerbarAndelDto {

    @Valid
    @NotNull
    private FordelFastsatteVerdierDto fastsatteVerdier;
    @ValidKodeverk
    private Inntektskategori forrigeInntektskategori;
    private Integer forrigeRefusjonPrÅr;
    private Integer forrigeArbeidsinntektPrÅr;

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
