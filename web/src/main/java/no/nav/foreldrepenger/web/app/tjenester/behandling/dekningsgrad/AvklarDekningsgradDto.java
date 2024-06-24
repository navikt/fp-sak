package no.nav.foreldrepenger.web.app.tjenester.behandling.dekningsgrad;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_DEKNINGSGRAD_KODE)
public class AvklarDekningsgradDto extends BekreftetAksjonspunktDto {

    @Min(80)
    @Max(100)
    private int dekningsgrad;

    public AvklarDekningsgradDto(String begrunnelse, int dekningsgrad) {
        super(begrunnelse);
        this.dekningsgrad = dekningsgrad;
    }

    AvklarDekningsgradDto() {
        // jackson
    }

    public int getDekningsgrad() {
        return dekningsgrad;
    }
}
