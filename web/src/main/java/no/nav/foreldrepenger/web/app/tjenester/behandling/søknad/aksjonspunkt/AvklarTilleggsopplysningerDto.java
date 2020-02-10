package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_TILLEGGSOPPLYSNINGER_KODE)
public class AvklarTilleggsopplysningerDto extends BekreftetAksjonspunktDto {


    @SuppressWarnings("unused") // NOSONAR
    private AvklarTilleggsopplysningerDto() {
        super();
        //For Jackson
    }

    public AvklarTilleggsopplysningerDto(String begrunnelse) { // NOSONAR
        super(begrunnelse);
    }


}
