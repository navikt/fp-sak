package no.nav.foreldrepenger.domene.opptjening.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_PERIODER_MED_OPPTJENING_KODE)
public class AvklarAktivitetsPerioderDto extends BekreftetAksjonspunktDto {

    @Size(max = 1000)
    private List<@Valid AvklarOpptjeningAktivitetDto> opptjeningsaktiviteter;

    @SuppressWarnings("unused")
    private AvklarAktivitetsPerioderDto() {
        super();
        // For Jackson
    }

    public AvklarAktivitetsPerioderDto(String begrunnelse, List<AvklarOpptjeningAktivitetDto> opptjeningsaktiviteter) {
        super(begrunnelse);
        this.opptjeningsaktiviteter = opptjeningsaktiviteter;
    }

    public List<AvklarOpptjeningAktivitetDto> getOpptjeningsaktiviteter() {
        return opptjeningsaktiviteter;
    }

}
