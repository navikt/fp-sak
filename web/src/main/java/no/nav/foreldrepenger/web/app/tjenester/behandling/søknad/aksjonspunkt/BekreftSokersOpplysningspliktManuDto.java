package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AvslagbartAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU_KODE)
public class BekreftSokersOpplysningspliktManuDto extends BekreftetAksjonspunktDto implements AvslagbartAksjonspunktDto{


    @JsonProperty("erVilkarOk")
    private Boolean erVilkarOk;
    
    @JsonProperty("inntektsmeldingerSomIkkeKommer")
    @Valid
    @Size(max = 50)
    private List<InntektsmeldingSomIkkeKommerDto> inntektsmeldingerSomIkkeKommer;

    BekreftSokersOpplysningspliktManuDto() { // NOSONAR
        // For Jackson
    }

    public BekreftSokersOpplysningspliktManuDto(String begrunnelse, Boolean erVilkarOk, List<InntektsmeldingSomIkkeKommerDto> inntektsmeldingerSomIkkeKommer) {
        super(begrunnelse);
        this.erVilkarOk = erVilkarOk;
        this.inntektsmeldingerSomIkkeKommer = inntektsmeldingerSomIkkeKommer;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        return null; // støttes ikke
    }

    @Override
    public Boolean getErVilkarOk() {
        return erVilkarOk;
    }

    public List<InntektsmeldingSomIkkeKommerDto> getInntektsmeldingerSomIkkeKommer() {
        return inntektsmeldingerSomIkkeKommer;
    }
}
