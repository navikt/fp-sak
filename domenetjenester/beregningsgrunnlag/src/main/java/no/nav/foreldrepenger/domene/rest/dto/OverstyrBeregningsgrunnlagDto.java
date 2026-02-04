package no.nav.foreldrepenger.domene.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE)
public class OverstyrBeregningsgrunnlagDto extends OverstyringAksjonspunktDto {

    @Valid
    private FaktaBeregningLagreDto fakta;

    @NotNull
    private List<@Valid FastsettBeregningsgrunnlagAndelDto> overstyrteAndeler;

    @SuppressWarnings("unused")
    private OverstyrBeregningsgrunnlagDto() {
        super();
        // For Jackson
    }

    public OverstyrBeregningsgrunnlagDto(List<FastsettBeregningsgrunnlagAndelDto> overstyrteAndeler, String begrunnelse) {
        super(begrunnelse);
        this.overstyrteAndeler = overstyrteAndeler;
    }


    @JsonIgnore
    @Override
    public String getAvslagskode() {
        return null;
    }

    @JsonIgnore
    @Override
    public boolean getErVilkarOk() {
        return true;
    }


    public FaktaBeregningLagreDto getFakta() {
        return fakta;
    }

    public List<FastsettBeregningsgrunnlagAndelDto> getOverstyrteAndeler() {
        return overstyrteAndeler;
    }
}
