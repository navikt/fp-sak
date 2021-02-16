package no.nav.foreldrepenger.web.app.tjenester.behandling.risikoklassifisering;


import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FARESIGNALER_KODE)
public class VurderFaresignalerDto extends BekreftetAksjonspunktDto {

    private Boolean harInnvirketBehandlingen;
    private FaresignalVurdering faresignalVurdering;

    VurderFaresignalerDto() {
        // For Jackson
    }

    public VurderFaresignalerDto(String begrunnelse, Boolean harInnvirketBehandlingen, FaresignalVurdering faresignalVurdering) {
        super(begrunnelse);
        this.harInnvirketBehandlingen = harInnvirketBehandlingen;
        this.faresignalVurdering = faresignalVurdering;
    }


    public Boolean getHarInnvirketBehandlingen() {
        return harInnvirketBehandlingen;
    }

    public void setHarInnvirketBehandlingen(Boolean harInnvirketBehandlingen) {
        this.harInnvirketBehandlingen = harInnvirketBehandlingen;
    }

    public FaresignalVurdering getFaresignalVurdering() {
        return faresignalVurdering;
    }

    public void setFaresignalVurdering(FaresignalVurdering faresignalVurdering) {
        this.faresignalVurdering = faresignalVurdering;
    }
}
