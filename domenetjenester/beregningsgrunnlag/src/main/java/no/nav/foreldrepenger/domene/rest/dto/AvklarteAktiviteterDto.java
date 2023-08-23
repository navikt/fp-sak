package no.nav.foreldrepenger.domene.rest.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

import java.util.List;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_AKTIVITETER_KODE)
public class AvklarteAktiviteterDto extends BekreftetAksjonspunktDto {


    @Valid
    @Size(max = 1000)
    private List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList;

    AvklarteAktiviteterDto() {
        // For Jackson
        super();
    }

    public AvklarteAktiviteterDto(List<BeregningsaktivitetLagreDto> beregningsaktivitetLagreDtoList, String begrunnelse) {
        super(begrunnelse);
        this.beregningsaktivitetLagreDtoList = beregningsaktivitetLagreDtoList;
    }


    public List<BeregningsaktivitetLagreDto> getBeregningsaktivitetLagreDtoList() {
        return beregningsaktivitetLagreDtoList;
    }
}
