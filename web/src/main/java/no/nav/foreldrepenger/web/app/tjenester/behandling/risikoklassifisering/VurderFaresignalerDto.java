package no.nav.foreldrepenger.web.app.tjenester.behandling.risikoklassifisering;


import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FARESIGNALER_KODE)
public class VurderFaresignalerDto extends BekreftetAksjonspunktDto {

    @NotNull
    @Valid
    private FaresignalVurdering faresignalVurdering;

    VurderFaresignalerDto() {
        // For Jackson
    }

    public VurderFaresignalerDto(String begrunnelse, FaresignalVurdering faresignalVurdering) {
        super(begrunnelse);
        this.faresignalVurdering = faresignalVurdering;
    }

    public FaresignalVurdering getFaresignalVurdering() {
        return faresignalVurdering;
    }
}
