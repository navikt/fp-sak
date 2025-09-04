package no.nav.foreldrepenger.domene.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public class FastsattBrukersAndel {

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long andelsnr;
    @NotNull
    private Boolean nyAndel;
    private Boolean lagtTilAvSaksbehandler;
    @NotNull
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private Integer fastsattBeløp;
    @NotNull
    @ValidKodeverk
    private Inntektskategori inntektskategori;

    FastsattBrukersAndel() {
        // For Jackson
    }

    public FastsattBrukersAndel(Boolean nyAndel,
                                Long andelsnr,
                                Boolean lagtTilAvSaksbehandler,
                                Integer fastsattBeløp,
                                Inntektskategori inntektskategori) {
        this.fastsattBeløp = fastsattBeløp;
        this.inntektskategori = inntektskategori;
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
        this.andelsnr = andelsnr;
        this.nyAndel = nyAndel;
    }


    public Long getAndelsnr() {
        return andelsnr;
    }

    public void setAndelsnr(Long andelsnr) {
        this.andelsnr = andelsnr;
    }

    public Boolean getNyAndel() {
        return nyAndel;
    }

    public void setNyAndel(Boolean nyAndel) {
        this.nyAndel = nyAndel;
    }

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

    public void setLagtTilAvSaksbehandler(Boolean lagtTilAvSaksbehandler) {
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
    }

    public Integer getFastsattBeløp() {
        return fastsattBeløp;
    }

    public void setFastsattBeløp(Integer fastsattBeløp) {
        this.fastsattBeløp = fastsattBeløp;
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    public void setInntektskategori(Inntektskategori inntektskategori) {
        this.inntektskategori = inntektskategori;
    }
}
