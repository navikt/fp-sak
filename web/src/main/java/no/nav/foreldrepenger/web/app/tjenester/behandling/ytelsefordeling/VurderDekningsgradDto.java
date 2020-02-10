package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_DEKNINGSGRAD_KODE)
public class VurderDekningsgradDto extends BekreftetAksjonspunktDto {


    @Min(80)
    @Max(100)
    @NotNull
    private int dekningsgrad;

    VurderDekningsgradDto() {
        // For Jackson
    }

    public VurderDekningsgradDto(String begrunnelse, int dekningsgrad) { // NOSONAR
        super(begrunnelse);
        this.dekningsgrad = dekningsgrad;
    }


    public int getDekningsgrad() {
        return dekningsgrad;
    }

}
