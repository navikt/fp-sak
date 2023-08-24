package no.nav.foreldrepenger.domene.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

import java.util.List;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER_KODE)
public class OverstyrBeregningsaktiviteterDto extends OverstyringAksjonspunktDto {


    @Valid
    @Size(max = 1000)
    private List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList;

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
