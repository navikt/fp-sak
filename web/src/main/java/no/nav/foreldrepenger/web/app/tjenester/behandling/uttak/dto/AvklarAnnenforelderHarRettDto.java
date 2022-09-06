package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT_KODE)
public class AvklarAnnenforelderHarRettDto extends BekreftetAksjonspunktDto {

    @NotNull
    private Boolean annenforelderHarRett;

    private Boolean annenForelderHarRettEØS;

    private Boolean annenforelderMottarUføretrygd;

    private Boolean annenforelderMottarStønadEØS;

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

    public Boolean getAnnenforelderMottarUføretrygd() {
        return annenforelderMottarUføretrygd;
    }

    public void setAnnenforelderMottarUføretrygd(Boolean annenforelderMottarUføretrygd) {
        this.annenforelderMottarUføretrygd = annenforelderMottarUføretrygd;
    }

    public Boolean getAnnenforelderMottarStønadEØS() {
        return annenforelderMottarStønadEØS;
    }

    public void setAnnenforelderMottarStønadEØS(Boolean annenforelderMottarStønadEØS) {
        this.annenforelderMottarStønadEØS = annenforelderMottarStønadEØS;
    }

    public Boolean getAnnenForelderHarRettEØS() {
        return annenForelderHarRettEØS;
    }

    public void setAnnenForelderHarRettEØS(Boolean annenForelderHarRettEØS) {
        this.annenForelderHarRettEØS = annenForelderHarRettEØS;
    }
}
