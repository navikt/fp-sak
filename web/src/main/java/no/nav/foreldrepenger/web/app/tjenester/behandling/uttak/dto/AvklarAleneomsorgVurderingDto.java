package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;


import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG_KODE)
public class AvklarAleneomsorgVurderingDto extends BekreftetAksjonspunktDto {

    @NotNull
    private Boolean aleneomsorg;

    private Boolean annenforelderHarRett;

    private Boolean annenForelderHarRettEØS;

    private Boolean annenforelderMottarUføretrygd;

    AvklarAleneomsorgVurderingDto() { // NOSONAR
        //For Jackson
    }

    public AvklarAleneomsorgVurderingDto(String begrunnelse) { // NOSONAR
        super(begrunnelse);
    }


    public Boolean getAleneomsorg() {
        return aleneomsorg;
    }

    public void setAleneomsorg(Boolean aleneomsorg) {
        this.aleneomsorg = aleneomsorg;
    }

    public Boolean getAnnenforelderHarRett() {
        return annenforelderHarRett;
    }

    public void setAnnenforelderHarRett(Boolean annenforelderHarRett) {
        this.annenforelderHarRett = annenforelderHarRett;
    }

    public Boolean getAnnenforelderMottarUføretrygd() {
        return annenforelderMottarUføretrygd;
    }

    public void setAnnenforelderMottarUføretrygd(Boolean annenforelderMottarUføretrygd) {
        this.annenforelderMottarUføretrygd = annenforelderMottarUføretrygd;
    }

    public Boolean getAnnenForelderHarRettEØS() {
        return annenForelderHarRettEØS;
    }

    public void setAnnenForelderHarRettEØS(Boolean annenForelderHarRettEØS) {
        this.annenForelderHarRettEØS = annenForelderHarRettEØS;
    }
}
