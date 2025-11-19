package no.nav.foreldrepenger.domene.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER_KODE)
public class OverstyrBeregningsaktiviteterDto extends OverstyringAksjonspunktDto {


    @Size(max = 1000)
    private List<@Valid BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList;

    @SuppressWarnings("unused")
    private OverstyrBeregningsaktiviteterDto() {
        super();
        // For Jackson
    }

    public OverstyrBeregningsaktiviteterDto(List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList, String begrunnelse) {
        super(begrunnelse);
        this.beregningsaktivitetLagreDtoList = beregningsaktivitetLagreDtoList;
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

    public List<BeregningsaktivitetLagreDto> getBeregningsaktivitetLagreDtoList() {
        return beregningsaktivitetLagreDtoList;
    }
}
