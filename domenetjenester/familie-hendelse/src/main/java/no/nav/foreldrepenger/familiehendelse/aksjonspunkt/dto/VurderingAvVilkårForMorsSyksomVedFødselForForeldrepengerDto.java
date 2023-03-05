package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_OM_VILKÅR_FOR_SYKDOM_OPPFYLT_KODE)
public class VurderingAvVilkårForMorsSyksomVedFødselForForeldrepengerDto extends BekreftetAksjonspunktDto {


    @NotNull
    private Boolean erMorForSykVedFodsel;

    public VurderingAvVilkårForMorsSyksomVedFødselForForeldrepengerDto() {
        // Jackson
    }

    public VurderingAvVilkårForMorsSyksomVedFødselForForeldrepengerDto(String begrunnelse, boolean erMorForSykVedFodsel) {
        super(begrunnelse);
        this.erMorForSykVedFodsel = erMorForSykVedFodsel;
    }


    public Boolean getErMorForSykVedFodsel() {
        return erMorForSykVedFodsel;
    }
}
