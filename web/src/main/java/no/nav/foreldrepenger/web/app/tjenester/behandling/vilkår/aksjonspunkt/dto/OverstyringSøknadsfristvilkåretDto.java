package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_SØKNADSFRISTVILKÅRET_KODE)
public class OverstyringSøknadsfristvilkåretDto extends OverstyringAksjonspunktDto {


    @JsonProperty("erVilkarOk")
    private boolean erVilkarOk;

    @SuppressWarnings("unused")
    private OverstyringSøknadsfristvilkåretDto() {
        super();
        // For Jackson
    }

    public OverstyringSøknadsfristvilkåretDto(boolean erVilkarOk, String begrunnelse) {
        super(begrunnelse);
        this.erVilkarOk = erVilkarOk;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        return null;
    }

    @Override
    public boolean getErVilkarOk() {
        return erVilkarOk;
    }
}
