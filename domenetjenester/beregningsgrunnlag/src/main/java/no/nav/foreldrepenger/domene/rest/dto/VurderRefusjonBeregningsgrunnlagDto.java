package no.nav.foreldrepenger.domene.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_REFUSJON_BERGRUNN)
public class VurderRefusjonBeregningsgrunnlagDto extends BekreftetAksjonspunktDto {
    @Size(max = 100)
    private List<@Valid VurderRefusjonAndelBeregningsgrunnlagDto> fastsatteAndeler;

    VurderRefusjonBeregningsgrunnlagDto() {
        // Jackson
    }

    public VurderRefusjonBeregningsgrunnlagDto(List<VurderRefusjonAndelBeregningsgrunnlagDto> fastsatteAndeler, String begrunnelse) {
        super(begrunnelse);
        this.fastsatteAndeler = fastsatteAndeler;
    }


    public List<VurderRefusjonAndelBeregningsgrunnlagDto> getFastsatteAndeler() {
        return fastsatteAndeler;
    }
}
