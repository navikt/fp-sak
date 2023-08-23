package no.nav.foreldrepenger.domene.rest.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

import java.util.List;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_REFUSJON_BERGRUNN)
public class VurderRefusjonBeregningsgrunnlagDto extends BekreftetAksjonspunktDto {
    @Valid
    @Size(max = 100)
    private List<VurderRefusjonAndelBeregningsgrunnlagDto> fastsatteAndeler;

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
