package no.nav.foreldrepenger.web.app.tjenester.behandling.dekningsgrad;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_DEKNINGSGRAD_KODE)
public class AvklarDekningsgradOverstyringDto extends OverstyringAksjonspunktDto {

    @Min(80)
    @Max(100)
    private int dekningsgrad;

    public AvklarDekningsgradOverstyringDto(String begrunnelse, int dekningsgrad) {
        super(begrunnelse);
        this.dekningsgrad = dekningsgrad;
    }

    AvklarDekningsgradOverstyringDto() {
        // jackson
    }

    public int getDekningsgrad() {
        return dekningsgrad;
    }

    @Override
    @JsonIgnore
    public String getAvslagskode() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean getErVilkarOk() {
        return false;
    }
}
