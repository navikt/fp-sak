package no.nav.foreldrepenger.domene.rest.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

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
