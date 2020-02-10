package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT_KODE)
public class AvklarAnnenforelderHarRettDto extends BekreftetAksjonspunktDto {

    @NotNull
    private Boolean annenforelderHarRett;

    AvklarAnnenforelderHarRettDto() {
        // For Jackson
    }

    public AvklarAnnenforelderHarRettDto(String begrunnelse) {
        super(begrunnelse);
    }


    public void setAnnenforelderHarRett(Boolean annenforelderHarRett) {
        this.annenforelderHarRett = annenforelderHarRett;
    }

    public Boolean getAnnenforelderHarRett() {
        return annenforelderHarRett;
    }

}
