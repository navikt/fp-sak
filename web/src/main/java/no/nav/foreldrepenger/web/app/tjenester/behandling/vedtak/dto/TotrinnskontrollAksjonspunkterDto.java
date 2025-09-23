package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto;

import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.domene.uttak.UttakPeriodeEndringDto;

public class TotrinnskontrollAksjonspunkterDto {

    @NotNull private String aksjonspunktKode;
    @NotNull private List<TotrinnskontrollAktivitetDto> opptjeningAktiviteter;
    private TotrinnsBeregningDto beregningDto;
    private String besluttersBegrunnelse;
    @NotNull private Boolean totrinnskontrollGodkjent;
    @NotNull private Set<VurderÅrsak> vurderPaNyttArsaker;
    @NotNull private List<UttakPeriodeEndringDto> uttakPerioder;

    public String getAksjonspunktKode() {
        return aksjonspunktKode;
    }

    public List<TotrinnskontrollAktivitetDto> getOpptjeningAktiviteter() {
        return opptjeningAktiviteter;
    }

    public String getBesluttersBegrunnelse() {
        return besluttersBegrunnelse;
    }

    public Boolean getTotrinnskontrollGodkjent() {
        return totrinnskontrollGodkjent;
    }

    public Set<VurderÅrsak> getVurderPaNyttArsaker() {
        return vurderPaNyttArsaker;
    }

    public TotrinnsBeregningDto getBeregningDto() {
        return beregningDto;
    }

    public List<UttakPeriodeEndringDto> getUttakPerioder() {
        return uttakPerioder;
    }

    public static class Builder {
        TotrinnskontrollAksjonspunkterDto kladd = new TotrinnskontrollAksjonspunkterDto();

        public Builder medAksjonspunktKode(String aksjonspunktKode) {
            kladd.aksjonspunktKode = aksjonspunktKode;
            return this;
        }

        public Builder medOpptjeningAktiviteter(List<TotrinnskontrollAktivitetDto> opptjeningAktiviteter) {
            kladd.opptjeningAktiviteter = opptjeningAktiviteter;
            return this;
        }

        public Builder medBeregningDto(TotrinnsBeregningDto beregningDto) {
            kladd.beregningDto = beregningDto;
            return this;
        }

        public Builder medBesluttersBegrunnelse(String besluttersBegrunnelse) {
            kladd.besluttersBegrunnelse = besluttersBegrunnelse;
            return this;
        }

        public Builder medTotrinnskontrollGodkjent(Boolean totrinnskontrollGodkjent) {
            kladd.totrinnskontrollGodkjent = totrinnskontrollGodkjent;
            return this;
        }

        public Builder medVurderPaNyttArsaker(Set<VurderÅrsak> vurderPaNyttArsaker) {
            kladd.vurderPaNyttArsaker = vurderPaNyttArsaker;
            return this;
        }

        public Builder medEndretUttakPerioder(List<UttakPeriodeEndringDto> uttakPerioder) {
            kladd.uttakPerioder = uttakPerioder;
            return this;
        }

        public TotrinnskontrollAksjonspunkterDto build() {
            return kladd;
        }
    }
}
