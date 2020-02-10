package no.nav.foreldrepenger.domene.opptjening.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_PERIODER_MED_OPPTJENING_KODE)
public class AvklarAktivitetsPerioderDto extends BekreftetAksjonspunktDto {


    @Valid
    @Size(max = 1000)
    private List<AvklarOpptjeningAktivitetDto> opptjeningAktivitetList;

    @SuppressWarnings("unused") // NOSONAR
    private AvklarAktivitetsPerioderDto() {
        super();
        // For Jackson
    }

    public AvklarAktivitetsPerioderDto(String begrunnelse, List<AvklarOpptjeningAktivitetDto> opptjeningAktivitetList) {
        super(begrunnelse);
        this.opptjeningAktivitetList = opptjeningAktivitetList;
    }


    public List<AvklarOpptjeningAktivitetDto> getOpptjeningAktivitetList() {
        return opptjeningAktivitetList;
    }

}
