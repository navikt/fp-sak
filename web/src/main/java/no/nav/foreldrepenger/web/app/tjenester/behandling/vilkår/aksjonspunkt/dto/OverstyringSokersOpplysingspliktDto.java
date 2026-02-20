package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.SØKERS_OPPLYSNINGSPLIKT_OVST_KODE)
public class OverstyringSokersOpplysingspliktDto extends OverstyringAksjonspunktDto {

    private boolean erVilkårOk;

    @SuppressWarnings("unused")
    private OverstyringSokersOpplysingspliktDto() {
        super();
        // For Jackson
    }

    public OverstyringSokersOpplysingspliktDto(boolean erVilkårOk, String begrunnelse) {
        super(begrunnelse);
        this.erVilkårOk = erVilkårOk;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        return null;
    }

    @Override
    public boolean getErVilkårOk() {
        return erVilkårOk;
    }

}
