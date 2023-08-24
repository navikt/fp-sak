package no.nav.foreldrepenger.domene.rest.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

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
