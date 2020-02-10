package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FAKTA_FOR_ATFL_SN_KODE)
public class VurderFaktaOmBeregningDto extends BekreftetAksjonspunktDto {


    @Valid
    @NotNull
    private FaktaBeregningLagreDto fakta;

    public VurderFaktaOmBeregningDto() {
        // For jackson
    }

    public VurderFaktaOmBeregningDto(String begrunnelse, FaktaBeregningLagreDto fakta) {
        super(begrunnelse);
        this.fakta = fakta;
    }

    public FaktaBeregningLagreDto getFakta() {
        return fakta;
    }


}
