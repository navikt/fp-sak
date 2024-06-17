package no.nav.foreldrepenger.domene.rest.dto;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;

/**
 * For å kunne identifisere andeler i fakta om beregning.
 * <p>
 * Enten andelsnr eller aktivitetstatus må vere satt.
 */
public class RedigerbarAndelFaktaOmBeregningDto {

    private Long andelsnr;
    private Boolean nyAndel;
    private AktivitetStatus aktivitetStatus;
    private Boolean lagtTilAvSaksbehandler;

    public RedigerbarAndelFaktaOmBeregningDto(AktivitetStatus aktivitetStatus) {
        this.nyAndel = true;
        this.lagtTilAvSaksbehandler = true;
        this.aktivitetStatus = aktivitetStatus;
    }

    public RedigerbarAndelFaktaOmBeregningDto(Boolean nyAndel, long andelsnr, Boolean lagtTilAvSaksbehandler) {
        this.nyAndel = nyAndel;
        this.andelsnr = andelsnr;
        this.lagtTilAvSaksbehandler = lagtTilAvSaksbehandler;
    }

    public Optional<AktivitetStatus> getAktivitetStatus() {
        return Optional.ofNullable(aktivitetStatus);
    }

    public Optional<Long> getAndelsnr() {
        return Optional.ofNullable(andelsnr);
    }

    public Boolean getNyAndel() {
        return nyAndel;
    }

    public Boolean getLagtTilAvSaksbehandler() {
        return lagtTilAvSaksbehandler;
    }

}
