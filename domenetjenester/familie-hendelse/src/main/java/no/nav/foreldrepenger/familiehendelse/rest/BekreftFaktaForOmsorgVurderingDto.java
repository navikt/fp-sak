package no.nav.foreldrepenger.familiehendelse.rest;


import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

public abstract class BekreftFaktaForOmsorgVurderingDto extends BekreftetAksjonspunktDto {

    BekreftFaktaForOmsorgVurderingDto() { // NOSONAR
        //For Jackson
    }

    public BekreftFaktaForOmsorgVurderingDto(String begrunnelse) { // NOSONAR
        super(begrunnelse);
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG_KODE)
    public static class BekreftAleneomsorgVurderingDto extends BekreftFaktaForOmsorgVurderingDto {


        @NotNull
        private Boolean aleneomsorg;

        @SuppressWarnings("unused") // NOSONAR
        private BekreftAleneomsorgVurderingDto() {
            // For Jackson
        }

        public BekreftAleneomsorgVurderingDto(String begrunnelse) { // NOSONAR
            super(begrunnelse);
        }

        public Boolean getAleneomsorg() {
            return aleneomsorg;
        }

        public void setAleneomsorg(Boolean aleneomsorg) {
            this.aleneomsorg = aleneomsorg;
        }

    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG_KODE)
    public static class BekreftOmsorgVurderingDto extends BekreftFaktaForOmsorgVurderingDto {


        @NotNull
        private Boolean omsorg;

        @Valid
        @Size(max = 50)
        private List<PeriodeDto> ikkeOmsorgPerioder;

        @SuppressWarnings("unused") // NOSONAR
        private BekreftOmsorgVurderingDto() {
            // For Jackson
        }

        public BekreftOmsorgVurderingDto(String begrunnelse) { // NOSONAR
            super(begrunnelse);
        }

        public Boolean getOmsorg() {
            return omsorg;
        }

        public void setOmsorg(Boolean omsorg) {
            this.omsorg = omsorg;
        }

        public List<PeriodeDto> getIkkeOmsorgPerioder() {
            return ikkeOmsorgPerioder;
        }

        public void setIkkeOmsorgPerioder(List<PeriodeDto> ikkeOmsorgPerioder) {
            this.ikkeOmsorgPerioder = ikkeOmsorgPerioder;
        }
    }

}
