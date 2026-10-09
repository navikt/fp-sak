package no.nav.foreldrepenger.web.app.tjenester.behandling.dekningsgrad;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_DEKNINGSGRAD_KODE)
public class AvklarDekningsgradOverstyringDto extends OverstyringAksjonspunktDto {

    @NotNull
    @Min(80)
    @Max(100)
    private Integer dekningsgrad;

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
    public boolean getErVilkårOk() {
        return false;
    }
}
