package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AvslagbartAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET_KODE)
public class SoknadsfristAksjonspunktDto extends BekreftetAksjonspunktDto implements AvslagbartAksjonspunktDto {


    @JsonProperty("erVilkarOk")
    @NotNull
    private Boolean erVilkarOk;

    SoknadsfristAksjonspunktDto() {
        //For Jackson
    }

    public SoknadsfristAksjonspunktDto(String begrunnelse, Boolean erVilkarOk) {
        super(begrunnelse);
        this.erVilkarOk = erVilkarOk;
    }

    @Override
    public Boolean getErVilkarOk() {
        return erVilkarOk;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        // Ikke supportert
        return null;
    }

}
