package no.nav.foreldrepenger.web.app.tjenester.behandling.risikoklassifisering;


import com.fasterxml.jackson.annotation.JsonTypeName;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FARESIGNALER_KODE)
public class VurderFaresignalerDto extends BekreftetAksjonspunktDto {

    private Boolean harInnvirketBehandlingen;

    VurderFaresignalerDto() {
        // For Jackson
    }

    public VurderFaresignalerDto(String begrunnelse, Boolean harInnvirketBehandlingen) {
        super(begrunnelse);
        this.harInnvirketBehandlingen = harInnvirketBehandlingen;
    }


    public Boolean getHarInnvirketBehandlingen() {
        return harInnvirketBehandlingen;
    }

    public void setHarInnvirketBehandlingen(Boolean harInnvirketBehandlingen) {
        this.harInnvirketBehandlingen = harInnvirketBehandlingen;
    }
}
