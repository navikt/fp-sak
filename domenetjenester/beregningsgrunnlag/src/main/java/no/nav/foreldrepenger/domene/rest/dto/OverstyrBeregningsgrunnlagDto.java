package no.nav.foreldrepenger.domene.rest.dto;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.domene.rest.historikk.Lønnsendring;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE)
public class OverstyrBeregningsgrunnlagDto extends OverstyringAksjonspunktDto {


    @Valid
    private FaktaBeregningLagreDto fakta;

    @Valid
    @NotNull
    private List<FastsettBeregningsgrunnlagAndelDto> overstyrteAndeler;

    private Set<Lønnsendring> endringer;

    @SuppressWarnings("unused") // NOSONAR
    private OverstyrBeregningsgrunnlagDto() {
        super();
        // For Jackson
    }

    public OverstyrBeregningsgrunnlagDto(List<FastsettBeregningsgrunnlagAndelDto> overstyrteAndeler, String begrunnelse) { // NOSONAR
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

    public Set<Lønnsendring> getEndringer() {
        return endringer;
    }

    public void setEndringer(Set<Lønnsendring> endringer) {
        this.endringer = endringer;
    }
}
