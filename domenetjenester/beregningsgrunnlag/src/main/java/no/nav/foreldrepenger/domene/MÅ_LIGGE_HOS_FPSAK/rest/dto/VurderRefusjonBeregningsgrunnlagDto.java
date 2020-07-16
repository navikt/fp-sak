package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.List;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_REFUSJON_BERGRUNN)
public class VurderRefusjonBeregningsgrunnlagDto extends BekreftetAksjonspunktDto {
    @Valid
    @Size(max = 100)
    private List<VurderRefusjonAndelBeregningsgrunnlagDto> fastsatteAndeler;

    VurderRefusjonBeregningsgrunnlagDto() { // NOSONAR
        // Jackson
    }

    public VurderRefusjonBeregningsgrunnlagDto(List<VurderRefusjonAndelBeregningsgrunnlagDto> fastsatteAndeler, String begrunnelse) { // NOSONAR
        super(begrunnelse);
        this.fastsatteAndeler = fastsatteAndeler;
    }


    public List<VurderRefusjonAndelBeregningsgrunnlagDto> getFastsatteAndeler() {
        return fastsatteAndeler;
    }
}
