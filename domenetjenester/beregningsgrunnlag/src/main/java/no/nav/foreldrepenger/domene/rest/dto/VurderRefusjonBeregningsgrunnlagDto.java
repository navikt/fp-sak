package no.nav.foreldrepenger.domene.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_REFUSJON_BERGRUNN)
public class VurderRefusjonBeregningsgrunnlagDto extends BekreftetAksjonspunktDto {
    @Valid
    @Size(max = 100)
    private List<VurderRefusjonAndelBeregningsgrunnlagDto> fastsatteAndeler;

    @Valid
    @Size
    private List<RefusjonskravForSentDto> refusjonskravForSentListe;

    VurderRefusjonBeregningsgrunnlagDto() {
        // Jackson
    }

    public VurderRefusjonBeregningsgrunnlagDto(List<VurderRefusjonAndelBeregningsgrunnlagDto> fastsatteAndeler,
                                               List<RefusjonskravForSentDto> refusjonskravForSentListe,
                                               String begrunnelse) {
        super(begrunnelse);
        this.fastsatteAndeler = fastsatteAndeler;
        this.refusjonskravForSentListe = refusjonskravForSentListe;
    }

    public List<VurderRefusjonAndelBeregningsgrunnlagDto> getFastsatteAndeler() {
        return fastsatteAndeler;
    }

    public List<RefusjonskravForSentDto> getRefusjonskravForSentListe() {
        return refusjonskravForSentListe;
    }
}
