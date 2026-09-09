package no.nav.foreldrepenger.domene.rest.dto.fordeling;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public class FordelBeregningsgrunnlagAndelDto {

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;
    @Pattern(regexp = InputValideringRegex.ARBEIDSGIVER)
    private String arbeidsgiverId;
    private UUID arbeidsforholdId;
    @NotNull
    private Boolean nyAndel;
    @ValidKodeverk
    private AndelKilde kilde;
    @ValidKodeverk
    private AktivitetStatus aktivitetStatus;
    @Valid
    @NotNull
    private FordelFastsatteVerdierDto fastsatteVerdier;
    @ValidKodeverk
    private Inntektskategori forrigeInntektskategori;
    @Min(0)
    @Max(178956970)
    private Integer forrigeRefusjonPrÅr;
    @Min(0)
    @Max(178956970)
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

    public boolean setNyAndel(boolean nyAndel) {
        return this.nyAndel = nyAndel;
    }

    public Long getAndelsnr() {
        return andelsnr;
    }

    public String getArbeidsgiverId() {
        return arbeidsgiverId;
    }

    public InternArbeidsforholdRef getArbeidsforholdId() {
        return InternArbeidsforholdRef.ref(arbeidsforholdId);
    }

    public Boolean getNyAndel() {
        return nyAndel;
    }

    public AndelKilde getKilde() {
        return kilde;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
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
